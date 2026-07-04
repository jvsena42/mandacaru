package com.github.jvsena42.mandacaru.data.update

import android.util.Log
import com.github.jvsena42.mandacaru.BuildConfig
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.data.AppUpdateRepository
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.UpdateStatus
import com.github.jvsena42.mandacaru.domain.model.github.GithubRelease
import com.github.jvsena42.mandacaru.domain.update.VersionComparator
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AppUpdateRepositoryImpl(
    private val gson: Gson,
    private val preferencesDataSource: PreferencesDataSource,
) : AppUpdateRepository {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val _updateStatus = MutableStateFlow(UpdateStatus())
    override val updateStatus = _updateStatus.asStateFlow()

    override suspend fun refresh(force: Boolean) = withContext(Dispatchers.IO) {
        emitCachedStatus()
        if (!force && !isCheckDue()) return@withContext
        _updateStatus.update { it.copy(isChecking = true, checkFailed = false) }
        runSuspendCatching { fetchLatestRelease() }
            .onSuccess { release -> applyRelease(release) }
            .onFailure { error ->
                Log.w(TAG, "refresh: failed to check for updates", error)
                _updateStatus.update { it.copy(isChecking = false, checkFailed = true) }
            }
        Unit
    }

    override suspend fun markUpdateSeen() {
        val latest = _updateStatus.value.latestVersion
        if (latest.isEmpty()) return
        preferencesDataSource.setString(PreferenceKeys.UPDATE_SEEN_VERSION, latest)
        _updateStatus.update { it.copy(isBadgeVisible = false) }
    }

    private suspend fun emitCachedStatus() {
        val latest = preferencesDataSource.getString(PreferenceKeys.UPDATE_LATEST_VERSION, "")
        if (latest.isEmpty()) return
        val apkUrl = preferencesDataSource.getString(PreferenceKeys.UPDATE_LATEST_APK_URL, "")
        val seen = preferencesDataSource.getString(PreferenceKeys.UPDATE_SEEN_VERSION, "")
        val isUpdate = VersionComparator.isNewer(latest, BuildConfig.VERSION_NAME)
        _updateStatus.update {
            it.copy(
                isUpdateAvailable = isUpdate,
                latestVersion = latest,
                apkDownloadUrl = apkUrl.ifEmpty { null },
                isBadgeVisible = isUpdate && latest != seen,
            )
        }
    }

    private suspend fun applyRelease(release: GithubRelease) {
        val latest = release.tagName.orEmpty().removePrefix("v").removePrefix("V")
        val apkUrl = release.assets
            .firstOrNull { it.name?.endsWith(APK_SUFFIX) == true }
            ?.browserDownloadUrl
        val isUpdate = latest.isNotEmpty() &&
            VersionComparator.isNewer(latest, BuildConfig.VERSION_NAME)
        val seen = preferencesDataSource.getString(PreferenceKeys.UPDATE_SEEN_VERSION, "")

        preferencesDataSource.setString(PreferenceKeys.UPDATE_LATEST_VERSION, latest)
        preferencesDataSource.setString(PreferenceKeys.UPDATE_LATEST_APK_URL, apkUrl.orEmpty())
        preferencesDataSource.setString(
            PreferenceKeys.UPDATE_LAST_CHECK,
            System.currentTimeMillis().toString(),
        )

        _updateStatus.update {
            it.copy(
                isUpdateAvailable = isUpdate,
                latestVersion = latest,
                apkDownloadUrl = apkUrl,
                releasePageUrl = release.htmlUrl ?: UpdateStatus.RELEASES_URL,
                isChecking = false,
                checkFailed = false,
                isBadgeVisible = isUpdate && latest != seen,
            )
        }
    }

    private fun fetchLatestRelease(): GithubRelease {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Unexpected response ${response.code}" }
            val body = response.body.string()
            return gson.fromJson(body, GithubRelease::class.java)
        }
    }

    private suspend fun isCheckDue(): Boolean {
        val lastCheck = preferencesDataSource
            .getString(PreferenceKeys.UPDATE_LAST_CHECK, "")
            .toLongOrNull() ?: 0L
        return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
    }

    companion object {
        private const val TAG = "AppUpdateRepository"
        private const val APK_SUFFIX = ".apk"
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val CHECK_INTERVAL_HOURS = 24L
        private val CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(CHECK_INTERVAL_HOURS)
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/jvsena42/mandacaru/releases/latest"
    }
}
