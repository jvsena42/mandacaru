package com.github.jvsena42.mandacaru.domain.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
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
    }

    private val dm =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    @Suppress(
        "LongMethod",
        "CyclomaticComplexMethod"
    )
    fun resolve(
        status: com.github.jvsena42.mandacaru.domain.model.UpdateStatus,
        downloadId: Long?
    ): UpdateState {

        android.util.Log.d(
            "UpdateResolver",
            "resolve version=${status.latestVersion} downloadId=$downloadId"
        )

        if (!status.isUpdateAvailable) {
            return UpdateState.Idle
        }

        val downloadManager = dm ?: return UpdateState.Available

        // Already fully downloaded (persistent state)
        if (registry.isDownloaded(status.latestVersion)) {
            val uri = registry.getCompletedUri(status.latestVersion)

            return if (uri != null && uriExists(uri)) {
                UpdateState.ReadyToInstall(uri)
            } else {
                registry.clearCompletedVersion(status.latestVersion)
                UpdateState.Available
            }
        }

        // Registry may have been cleared while DownloadManager survived.
        if (downloadId == null) {
            findCompletedDownload(status.apkDownloadUrl)?.let {
                return UpdateState.ReadyToInstall(it)
            }

            return UpdateState.Available
        }

        val cursor =
            downloadManager.query(
                DownloadManager.Query().setFilterById(downloadId)
            ) ?: return UpdateState.Available

        cursor.use {
            if (!it.moveToFirst()) {
                return UpdateState.Available
            }

            val statusIndex =
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)

            val downloadStatus = it.getInt(statusIndex)

            android.util.Log.d(
                "UpdateResolver",
                "DownloadManager status=$downloadStatus downloadId=$downloadId"
            )

            return when (downloadStatus) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING -> {
                    val downloaded =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                            )
                        )

                    val total =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                            )
                        )

                    val progress =
                        if (total > 0) {
                            ((downloaded * PERCENTAGE_MAX) / total).toInt()
                        } else {
                            PERCENTAGE_MIN
                        }

                    UpdateState.Downloading(progress)
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uri =
                        it.getString(
                                it.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_LOCAL_URI
                                )
                            )
                            ?.let(Uri::parse)

                    if (uri != null && uriExists(uri)) {
                        UpdateState.ReadyToInstall(uri)
                    } else {
                        android.util.Log.d(
                            "UpdateResolver",
                            "DownloadManager reports successful but APK missing uri=$uri"
                        )
                        UpdateState.Available
                    }
                }

                else -> UpdateState.Available
            }
        }
    }

    private fun uriExists(uri: Uri): Boolean {
        return when (uri.scheme) {
            "file" -> File(uri.path ?: "").exists()
            else -> false
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
                        "UpdateResolver",
                        "Recovered completed download uri=$localUri"
                    )
                    return localUri
                }
            }
        }

        return null
    }
}
