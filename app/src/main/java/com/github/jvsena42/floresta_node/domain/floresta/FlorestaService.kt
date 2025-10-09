package com.github.jvsena42.floresta_node.domain.floresta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.jvsena42.floresta_node.FlorestaNodeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.jvm.java
import com.github.jvsena42.floresta_node.R
class FlorestaService : Service() {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val florestaDaemon: FlorestaDaemon by inject()

    override fun onCreate() {
        super.onCreate()
        startForeground(FLORESTA_NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val channelId = "floresta_service_channel"
        val channelName = "Floresta Service"

        // Create the NotificationChannel (for Android 8.0 and above)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )

        val stopIntent = Intent(this, FlorestaService::class.java).apply {
            action = ACTION_STOP_SERVICE_AND_APP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floresta Node")
            .setContentText("Floresta node is running in background")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_x,
                "Stop App",
                stopPendingIntent
            )

        return notificationBuilder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        try {
            when (intent?.action) {
                ACTION_STOP_SERVICE_AND_APP -> {
                    // Close all activities
                    FlorestaNodeApplication.currentActivity?.value?.finishAndRemoveTask()
                    // Stop the service
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            ioScope.launch {
                Log.d(TAG, "onStartCommand: ")
                florestaDaemon.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand error: ", e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ioScope.launch {
            Log.d(TAG, "onDestroy: ")
            florestaDaemon.stop()
        }
        ioScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "FlorestaService"
        private const val FLORESTA_NOTIFICATION_ID = 1000
        const val CHANNEL_ID = "floresta_notification_channel_id"
        const val ACTION_STOP_SERVICE_AND_APP = "com.github.jvsena42.floresta_node.domain.floresta.action.STOP_SERVICE_AND_APP"

    }
}