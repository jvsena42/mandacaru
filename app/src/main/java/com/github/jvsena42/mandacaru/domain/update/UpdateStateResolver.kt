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

        @Suppress("LongMethod")    
        fun resolve(
            status: com.github.jvsena42.mandacaru.domain.model.UpdateStatus,
            downloadId: Long?
        ): UpdateState {

android.util.Log.d(
    "UpdateResolver",
    "resolve version=${status.latestVersion} downloadId=$downloadId"
)
 
      val downloadManager = dm ?: run {
    android.util.Log.d(
        "UpdateResolver",
        "DownloadManager unavailable"
    )
    return UpdateState.Available
} 

if (!status.isUpdateAvailable) {
    android.util.Log.d(
        "UpdateResolver",
        "No update available"
    )
    return UpdateState.Idle
} 
        
            // Already fully downloaded (persistent state)
            if (registry.isDownloaded(status.latestVersion)) {
android.util.Log.d(
    "UpdateResolver",
    "Registry reports downloaded"
) 

			val uri = registry.getCompletedUri(status.latestVersion)
android.util.Log.d(
    "UpdateResolver",
    "Registry uri=$uri"
)                

return if (uri != null) {
    android.util.Log.d(
        "UpdateResolver",
        "Returning ReadyToInstall from registry uri=$uri"
    )
android.util.Log.d(
    "UpdateResolver",
    "SUCCESSFUL download detected, uri=$uri"
)    
UpdateState.ReadyToInstall(uri)
} else {
    android.util.Log.d(
        "UpdateResolver",
        "Registry says downloaded but uri missing"
    )
    UpdateState.Available
}
            }
    
if (downloadId == null) {
    android.util.Log.d(
        "UpdateResolver",
        "No active download id"
    )
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
    android.util.Log.d(
        "UpdateResolver",
        "Cursor empty"
    )
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
    android.util.Log.d(
        "UpdateResolver",
        "Returning Downloading status=$downloadStatus"
    )
    UpdateState.Downloading(progress = 0)
}

                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uriIndex =
                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)

                    val uriString = it.getString(uriIndex)
                    val uri = uriString?.let(Uri::parse)
android.util.Log.d(
    "UpdateResolver",
    "uriString=$uriString uri=$uri"
)

                    if (uri != null) {
    android.util.Log.d(
        "UpdateResolver",
        "Returning ReadyToInstall"
    )                    
    					UpdateState.ReadyToInstall(uri)
} else {
    android.util.Log.d(
        "UpdateResolver",
        "Successful download but uri is null"
    )
    UpdateState.Available
}
}

else -> {
    android.util.Log.d(
        "UpdateResolver",
        "Returning Available for DownloadManager status=$downloadStatus"
    )
    UpdateState.Available
}

            }
        }
    }
}
