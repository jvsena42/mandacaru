package com.github.jvsena42.mandacaru.domain.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.github.jvsena42.mandacaru.data.update.UpdateDownloadRegistry
import java.io.File

/**
 * Single responsibility:
 * Convert update status + registry + DownloadManager into UI state.
 */
class UpdateStateResolver(
    context: Context,
    private val registry: UpdateDownloadRegistry
) {

    private companion object {
        private const val PERCENTAGE_MIN = 0
        private const val PERCENTAGE_MAX = 100
        private const val TAG = "UpdateResolver"
    }

    private val dm =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    
    @Suppress("CyclomaticComplexMethod")
    fun resolve(
        status: com.github.jvsena42.mandacaru.domain.model.UpdateStatus,
        downloadId: Long?
    ): UpdateState {

        android.util.Log.d(
            TAG,
            "resolve version=${status.latestVersion} downloadId=$downloadId"
        )

        if (!status.isUpdateAvailable) {
            return UpdateState.Idle
        }

        if (registry.isDownloaded(status.latestVersion)) {
            val registryState = resolveRegistryDownload(status.latestVersion)

            if (registryState is UpdateState.ReadyToInstall) {
                return registryState
            }
        }        
        
        if (downloadId == null) {
            return findCompletedDownload(status.apkDownloadUrl)
                ?.let { UpdateState.ReadyToInstall(it) }
                ?: UpdateState.Available
        }

        return resolveDownloadManagerState(downloadId)
    }

    private fun resolveRegistryDownload(version: String): UpdateState? {
        val uri = registry.getCompletedUri(version)

        if (uri != null && uriExists(uri)) {
            return UpdateState.ReadyToInstall(uri)
        }

        if (uri != null) {
            registry.clearCompletedVersion(version)
        }

        return null
    }

    @Suppress("CyclomaticComplexMethod")
    private fun resolveDownloadManagerState(downloadId: Long): UpdateState {
        val downloadManager = dm ?: return UpdateState.Available

        val cursor =
            downloadManager.query(
                DownloadManager.Query().setFilterById(downloadId)
            ) ?: return UpdateState.Available

        cursor.use {
            if (!it.moveToFirst()) {
                return UpdateState.Available
            }

            val downloadStatus =
                it.getInt(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_STATUS
                    )
                )

            android.util.Log.d(
                TAG,
                "DownloadManager status=$downloadStatus downloadId=$downloadId"
            )

            return when (downloadStatus) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING -> {
                    resolveDownloadingState(it)
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    resolveSuccessfulDownload(it)
                }

                else -> UpdateState.Available
            }
        }
    }

    private fun resolveDownloadingState(cursor: android.database.Cursor): UpdateState {
        val downloaded =
            cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                )
            )

        val total =
            cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                )
            )

        val progress =
            if (total > 0) {
                ((downloaded * PERCENTAGE_MAX) / total).toInt()
            } else {
                PERCENTAGE_MIN
            }

        return UpdateState.Downloading(progress)
    }

    private fun resolveSuccessfulDownload(
        cursor: android.database.Cursor
    ): UpdateState {
        val uri =
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_LOCAL_URI
                )
            )?.let(Uri::parse)

        return if (uri != null && uriExists(uri)) {
            UpdateState.ReadyToInstall(uri)
        } else {
            android.util.Log.d(
                TAG,
                "DownloadManager reports successful but APK missing uri=$uri"
            )
            UpdateState.Available
        }
    }

    private fun uriExists(uri: Uri): Boolean {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: "").exists()
                "content" -> true
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun findCompletedDownload(url: String?): Uri? {
        if (url == null) return null

        val cursor =
            dm?.query(
                DownloadManager.Query()
                    .setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
            ) ?: return null

        cursor.use {
            val urlIndex =
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)

            val localUriIndex =
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)

            while (it.moveToNext()) {
                val existingUrl = it.getString(urlIndex)

                if (existingUrl != url) continue

                val localUri =
                    it.getString(localUriIndex)
                        ?.let(Uri::parse)

                if (localUri != null && uriExists(localUri)) {
                    android.util.Log.d(
                        TAG,
                        "Recovered completed download uri=$localUri"
                    )
                    return localUri
                }
            }
        }

        return null
    }
}

