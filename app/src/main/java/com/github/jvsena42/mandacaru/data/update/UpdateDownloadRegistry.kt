package com.github.jvsena42.mandacaru.data.update

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Persistent single source of truth for APK download tracking.
 * Uses SharedPreferences to remember completed downloads across app restarts.
 */
class UpdateDownloadRegistry(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("update_registry", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_DOWNLOAD_ID = "active_download_id"
        private const val KEY_ACTIVE_VERSION = "active_version"
        private const val KEY_COMPLETED_PREFIX = "completed_" // completed_<version> = uri string
    }

    // ----------------------------
    // MARK DOWNLOAD STARTED
    // ----------------------------
    fun markDownloading(version: String, downloadId: Long) {
        prefs.edit()
            .putLong(KEY_ACTIVE_DOWNLOAD_ID, downloadId)
            .putString(KEY_ACTIVE_VERSION, version)
            .apply()
    }

    // ----------------------------
    // MARK DOWNLOAD COMPLETED
    // ----------------------------
    fun markCompleted(downloadId: Long, uri: Uri) {
        val activeId = prefs.getLong(KEY_ACTIVE_DOWNLOAD_ID, -1L)
        // Verify the completed ID matches the current active download ID
        if (activeId == -1L || activeId != downloadId) return

        val version = prefs.getString(KEY_ACTIVE_VERSION, null) ?: return
        
        prefs.edit()
            .remove(KEY_ACTIVE_DOWNLOAD_ID)
            .remove(KEY_ACTIVE_VERSION)
            .putString("$KEY_COMPLETED_PREFIX$version", uri.toString())
            .apply()
    }

    // ----------------------------
    // MARK DOWNLOAD FAILED / CANCELLED
    // ----------------------------
    /** Clears the active download tracking state if a download fails or is cancelled */
    fun clearActiveDownload() {
        prefs.edit()
            .remove(KEY_ACTIVE_DOWNLOAD_ID)
            .remove(KEY_ACTIVE_VERSION)
            .apply()
    }

    /** Specific cleanup if a version is explicitly invalidated or deleted */
    fun clearCompletedVersion(version: String) {
        prefs.edit()
            .remove("$KEY_COMPLETED_PREFIX$version")
            .apply()
    }

    // ----------------------------
    // QUERY METHODS
    // ----------------------------

    /** Returns the currently active download ID, or null if none */
    fun getActiveDownloadId(): Long? {
        val id = prefs.getLong(KEY_ACTIVE_DOWNLOAD_ID, -1L)
        return if (id != -1L) id else null
    }

    /** Returns URI of a completed version, or null if not downloaded */
    fun getCompletedUri(version: String): Uri? {
        val uriString = prefs.getString("$KEY_COMPLETED_PREFIX$version", null)
        return uriString?.let(Uri::parse)
    }

    /** Is this version already downloaded? */
    fun isDownloaded(version: String): Boolean {
        return prefs.contains("$KEY_COMPLETED_PREFIX$version")
    }

    /** Is a download in progress for this version? */
    fun isDownloading(version: String): Boolean {
        val activeVersion = prefs.getString(KEY_ACTIVE_VERSION, null)
        val hasActiveId = prefs.contains(KEY_ACTIVE_DOWNLOAD_ID)
        return activeVersion == version && hasActiveId
    }
}
