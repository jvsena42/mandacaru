package com.github.jvsena42.mandacaru.data.update

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.jvsena42.mandacaru.data.update.UpdateDownloadRegistry
import java.io.File

class UpdateDownloadReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId =
            intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)

        if (downloadId == -1L) return

        val dm =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val cursor =
            dm.query(DownloadManager.Query().setFilterById(downloadId))
                ?: return

        cursor.use {

            if (!it.moveToFirst()) return

            val status =
                it.getInt(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_STATUS
                    )
                )

            if (status != DownloadManager.STATUS_SUCCESSFUL) return

            val uriString =
                it.getString(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_LOCAL_URI
                    )
                )

            val uri = uriString?.let(Uri::parse) ?: return

            if (!uriExists(uri)) {
                android.util.Log.d(
                    "UpdateReceiver",
                    "Ignoring completed download because APK is missing: $uri"
                )
                return
            }

            UpdateDownloadRegistry(context)
                .markCompleted(downloadId, uri)

            showUpdateReadyNotification(context)
        }
    }

    private fun showUpdateReadyNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for downloaded updates"
                }
            )
        }

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Update Ready")
                .setContentText("A new Mandacaru update is ready to install.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notification)
        }
    }

    private fun uriExists(uri: Uri): Boolean =
        when (uri.scheme) {
            "file" -> File(uri.path.orEmpty()).exists()
            else -> false
        }
}
