package com.github.jvsena42.mandacaru.data.geoip

import android.util.Log
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.data.GeoIpDatabaseRepository
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.geoip.GeoIpUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.YearMonth
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Downloads the DB-IP Lite country database, at most monthly, matching upstream's cadence.
 *
 * Uses OkHttp rather than [android.app.DownloadManager] on purpose: NetworkPolicyManager
 * enforces the WiFi-only preference by binding the *process* network, which an out-of-process
 * DownloadManager would sidestep — silently downloading over mobile data against the user's
 * setting. An in-process call inherits the binding.
 */
class GeoIpDatabaseRepositoryImpl(
    private val database: GeoIpDatabase,
    private val preferencesDataSource: PreferencesDataSource,
    private val networkPolicy: NetworkPolicy,
    private val cacheDir: File,
    private val currentMonth: () -> YearMonth = { YearMonth.now() },
) : GeoIpDatabaseRepository {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private sealed interface Outcome {
        object Installed : Outcome
        object NotFound : Outcome
        object Rejected : Outcome
    }

    override suspend fun refresh(force: Boolean) = withContext(Dispatchers.IO) {
        val month = currentMonth()
        if (isUpToDate(month)) return@withContext
        if (networkPolicy.isWaitingForWifi.value) {
            Log.i(TAG, "refresh: skipped, waiting for WiFi")
            return@withContext
        }
        if (!force && !isCheckDue()) return@withContext

        // Early in a month the new file may not be published yet; fall back one month.
        val months = listOf(month, month.minusMonths(1))
        for (candidate in months) {
            val outcome = runSuspendCatching { attempt(candidate) }
                .onFailure {
                    // Transport failure: leave the throttle untouched so the next launch retries.
                    Log.w(TAG, "refresh: download failed for ${GeoIpUrl.stamp(candidate)}", it)
                }
                .getOrNull() ?: return@withContext

            if (outcome == Outcome.Installed) {
                preferencesDataSource.setString(
                    PreferenceKeys.GEOIP_DB_MONTH,
                    GeoIpUrl.stamp(candidate),
                )
                markChecked()
                return@withContext
            }
        }

        // Every candidate answered definitively (404 or unusable); don't hammer the server.
        markChecked()
    }

    private suspend fun attempt(month: YearMonth): Outcome {
        val temp = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, cacheDir)
        var installed = false
        try {
            if (!downloadTo(GeoIpUrl.forMonth(month), temp)) return Outcome.NotFound
            // install() validates the candidate and moves it into place only if it is usable.
            installed = database.install(temp)
            return if (installed) Outcome.Installed else Outcome.Rejected
        } finally {
            // A no-op once install() has renamed or deleted it; this covers every other exit.
            if (!installed) temp.delete()
        }
    }

    /** Returns false when the month's file is not published; throws on transport failure. */
    private fun downloadTo(url: String, target: File): Boolean {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.code == HTTP_NOT_FOUND) return false
            check(response.isSuccessful) { "Unexpected response ${response.code}" }
            // Stream through the gunzip so the ~8 MB never lands on the heap.
            GZIPInputStream(response.body.byteStream()).use { input ->
                target.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        }
        return true
    }

    private suspend fun isUpToDate(month: YearMonth): Boolean =
        database.exists() &&
            preferencesDataSource.getString(PreferenceKeys.GEOIP_DB_MONTH, "") == GeoIpUrl.stamp(month)

    private suspend fun isCheckDue(): Boolean {
        val lastCheck = preferencesDataSource
            .getString(PreferenceKeys.GEOIP_LAST_CHECK, "")
            .toLongOrNull() ?: 0L
        return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
    }

    private suspend fun markChecked() {
        preferencesDataSource.setString(
            PreferenceKeys.GEOIP_LAST_CHECK,
            System.currentTimeMillis().toString(),
        )
    }

    private companion object {
        const val TAG = "GeoIpDatabaseRepo"
        const val TEMP_PREFIX = "geoip"
        const val TEMP_SUFFIX = ".mmdb.tmp"
        const val HTTP_NOT_FOUND = 404
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 120L
        const val CHECK_INTERVAL_DAYS = 30L
        val CHECK_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(CHECK_INTERVAL_DAYS)
    }
}
