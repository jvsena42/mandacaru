package com.github.jvsena42.mandacaru.domain.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.github.jvsena42.mandacaru.data.update.UpdateDownloadRegistry

/**
 * Single responsibility:
 * Convert update status + registry + DownloadManager into UI state.
 */
    class UpdateStateResolver(
        context: Context,
        private val registry: UpdateDownloadRegistry
    ) {
    
        private val dm =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    
        fun resolve(
            val downloadManager = dm ?: return UpdateState.Available
            status: com.github.jvsena42.mandacaru.domain.model.UpdateStatus,
            downloadId: Long?
        ): UpdateState {
    
            // No update available at all
            if (!status.isUpdateAvailable) {
                return UpdateState.Idle
            }
    
            // Already fully downloaded (persistent state)
            if (registry.isDownloaded(status.latestVersion)) {
                val uri = registry.getCompletedUri(status.latestVersion)
                return if (uri != null) {
                    UpdateState.ReadyToInstall(uri)
                } else {
                    UpdateState.Available
                }
            }
    
            // No active download
            if (downloadId == null) {
                return UpdateState.Available
            }
    
            val cursor = downloadManager.query(
                DownloadManager.Query().setFilterById(downloadId)
            ) ?: return UpdateState.Available

        cursor.use {
            if (!it.moveToFirst()) return UpdateState.Available

            val statusIndex =
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)

            return when (it.getInt(statusIndex)) {

                DownloadManager.STATUS_RUNNING ->
                    UpdateState.Downloading

                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uriIndex =
                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)

                    val uriString = it.getString(uriIndex)
                    val uri = uriString?.let(Uri::parse)

                    if (uri != null) {
                        UpdateState.ReadyToInstall(uri)
                    } else {
                        UpdateState.Available
                    }
                }

                else -> UpdateState.Available
            }
        }
    }
}
