package com.github.jvsena42.mandacaru.data.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class UpdateDownloadReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {

android.util.Log.d(
    "UpdateReceiver",
    "ENTER onReceive action=${intent.action}"
)
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        android.util.Log.d(
    "UpdateReceiver",
    "DOWNLOAD_COMPLETE received id=$downloadId"
)
        if (downloadId == -1L) return

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId)) ?: return

android.util.Log.d(
    "UpdateReceiver",
    "cursor found"
)

        cursor.use {
            if (!it.moveToFirst()) return

            val statusIndex = it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val status = it.getInt(statusIndex)
        android.util.Log.d(
    "UpdateReceiver",
    "DownloadManager completion status=$status"
)

            if (status != DownloadManager.STATUS_SUCCESSFUL) return

            val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val uriString = it.getString(uriIndex)
            val uri = uriString?.let(Uri::parse)

android.util.Log.d(
    "UpdateReceiver",
    "uriString=$uriString uri=$uri"
)

android.util.Log.d(
    "UpdateReceiver",
    "calling markCompleted"
)

            if (uri != null) {
                // Mark the download as completed in the registry
                val registry = UpdateDownloadRegistry(context)
android.util.Log.d(
    "UpdateReceiver",
    "About to mark completed id=$downloadId uri=$uri"
)
                registry.markCompleted(downloadId, uri)
android.util.Log.d(
    "UpdateReceiver",
    "markCompleted finished"
)

android.util.Log.d(
    "UpdateReceiver",
    "showing ready notification"
)

                // Show a persistent notification
                showUpdateReadyNotification(context)
            }
        }
    }

    private fun showUpdateReadyNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for downloaded updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
}
