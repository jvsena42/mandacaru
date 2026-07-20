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
 
    val downloadManager = dm ?: run {
    return UpdateState.Available
} 

if (!status.isUpdateAvailable) {
    return UpdateState.Idle
} 

// Already fully downloaded (persistent state)
if (registry.isDownloaded(status.latestVersion)) {

    val uri = registry.getCompletedUri(status.latestVersion)

    return if (uri != null && uriExists(uri)) {
        UpdateState.ReadyToInstall(uri)
    } else {
        UpdateState.Available
    }
}
    
if (downloadId == null) {
    return UpdateState.Available
} 
            val cursor = downloadManager.query(
                DownloadManager.Query().setFilterById(downloadId)

) ?: run {
    android.util.Log.d(
        "UpdateResolver",
        "DownloadManager query returned null"
    )
    return UpdateState.Available
}

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
    val downloadedIndex =
        it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

    val totalIndex =
        it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

    val downloaded =
        it.getLong(downloadedIndex)

    val total =
        it.getLong(totalIndex)

    val progress =
        if (total > 0) {
            ((downloaded * PERCENTAGE_MAX) / total).toInt()
        } else {
            PERCENTAGE_MIN
        }

    android.util.Log.d(
        "UpdateResolver",
        "Downloading $downloaded/$total bytes progress=$progress%"
    )

    UpdateState.Downloading(progress)
}

DownloadManager.STATUS_SUCCESSFUL -> {
    val uriIndex =
        it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)

    val uriString = it.getString(uriIndex)
    val uri = uriString?.let(Uri::parse)

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

else -> {
    UpdateState.Available
}

            }
        }
    }

    private fun uriExists(uri: Uri): Boolean {
        return when (uri.scheme) {
            "file" -> {
                java.io.File(uri.path ?: "").exists()
            }
            else -> {
                false
            }
        }
    }
}
