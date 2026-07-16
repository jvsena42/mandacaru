package com.github.jvsena42.mandacaru.data.geoip

import android.util.Log
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.data.GeoIpDatabaseRepository
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.geoip.GeoIpUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        if (!force && !isCheckDue()) return@withContext

        // NetworkPolicyManager assumes "no WiFi" synchronously in Application.onCreate and only
        // clears the flag from its async onAvailable callback, so this is true on essentially
        // every cold start. Suspend until WiFi arrives rather than reading it once and giving
        // up — refresh() is called once per process, so a skip here would mean never.
        networkPolicy.isWaitingForWifi.first { !it }

        val installedMonth = preferencesDataSource.getString(PreferenceKeys.GEOIP_DB_MONTH, "")
        // Early in a month the new file may not be published yet; fall back one month.
        val months = listOf(month, month.minusMonths(1))
        var transportFailed = false

        for (candidate in months) {
            // Never re-download the file we already hold (the fallback month can be exactly it).
            if (database.exists() && GeoIpUrl.stamp(candidate) == installedMonth) {
                markChecked()
                return@withContext
            }
            val outcome = runSuspendCatching { attempt(candidate) }
                .onFailure {
                    Log.w(TAG, "refresh: download failed for ${GeoIpUrl.stamp(candidate)}", it)
                    transportFailed = true
                }
                .getOrNull()

            if (outcome == Outcome.Installed) {
                preferencesDataSource.setString(
                    PreferenceKeys.GEOIP_DB_MONTH,
                    GeoIpUrl.stamp(candidate),
                )
                markChecked()
                return@withContext
            }
        }

        // Only throttle when the server actually answered (404 or an unusable body). A transport
        // failure taught us nothing, so leave the throttle untouched and retry on the next launch.
        if (!transportFailed) markChecked()
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
