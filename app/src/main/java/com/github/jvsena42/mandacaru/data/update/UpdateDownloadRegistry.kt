package com.github.jvsena42.mandacaru.data.update

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persistent registry for APK download tracking.
 * Uses SharedPreferences to remember active and completed downloads across app restarts.
 */
class UpdateDownloadRegistry(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("update_registry", Context.MODE_PRIVATE)
    private val _changes = MutableStateFlow(0)
    val changes = _changes.asStateFlow()

    companion object {
        private const val KEY_ACTIVE_DOWNLOAD_ID = "active_download_id"
        private const val KEY_ACTIVE_VERSION = "active_version"
        private const val KEY_COMPLETED_PREFIX = "completed_" // completed_<version> = uri string
    }

    // ----------------------------
    // MARK DOWNLOAD STARTED
    // ----------------------------

    fun markDownloading(version: String, downloadId: Long) {
        
android.util.Log.d(
   "UpdateRegistry",
    "markDownloading version=$version id=$downloadId"
)

        prefs.edit()
            .putLong(KEY_ACTIVE_DOWNLOAD_ID, downloadId)
            .putString(KEY_ACTIVE_VERSION, version)
            .apply()

        _changes.update { it + 1 }
    }

    // ----------------------------
    // MARK DOWNLOAD COMPLETED
    // ----------------------------
    fun markCompleted(downloadId: Long, uri: Uri) {
        android.util.Log.d(
             "UpdateRegistry",
             "markCompleted called id=$downloadId uri=$uri"
        )    

        val activeId = prefs.getLong(KEY_ACTIVE_DOWNLOAD_ID, -1L)

android.util.Log.d(
    "UpdateRegistry",
    "activeId=$activeId"
)

        if (activeId == -1L || activeId != downloadId) return

        val version = prefs.getString(KEY_ACTIVE_VERSION, null) ?: return

android.util.Log.d(
    "UpdateRegistry",
    "activeVersion=$version"
)

        prefs.edit()
            .remove(KEY_ACTIVE_DOWNLOAD_ID)
            .remove(KEY_ACTIVE_VERSION)
            .putString("$KEY_COMPLETED_PREFIX$version", uri.toString())
            .apply()

android.util.Log.d(
    "UpdateRegistry",
    "completed saved version=$version uri=$uri"
)

        _changes.update { it + 1 }
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

        _changes.update { it + 1 }
    }

    /** Specific cleanup if a version is explicitly invalidated or deleted */
    fun clearCompletedVersion(version: String) {
        prefs.edit()
            .remove("$KEY_COMPLETED_PREFIX$version")
            .apply()
    
        _changes.update { it + 1 }
    }

    // ----------------------------
    // QUERY METHODS
    // ----------------------------

    /** Returns the currently active download ID, or null if none */
    fun getActiveDownloadId(): Long? {
        val id = prefs.getLong(KEY_ACTIVE_DOWNLOAD_ID, -1L)
        return if (id != -1L) id else null
    }

    fun refresh() {
        _changes.update { it + 1 }
    }

    /** Returns URI of a completed version, or null if not downloaded */
    fun getCompletedUri(version: String): Uri? {
        val uriString = prefs.getString("$KEY_COMPLETED_PREFIX$version", null)
        return uriString?.let(Uri::parse)
    }

    /** Is this version already downloaded? */
    fun isDownloaded(version: String): Boolean {
        return getCompletedUri(version) != null
    }

    /** Is a download in progress for this version? */
    fun isDownloading(version: String): Boolean {
        val activeVersion = prefs.getString(KEY_ACTIVE_VERSION, null)
        val hasActiveId = prefs.contains(KEY_ACTIVE_DOWNLOAD_ID)
        return activeVersion == version && hasActiveId
    }
}

