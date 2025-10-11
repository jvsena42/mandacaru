package com.github.jvsena42.floresta_node.domain.floresta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.jvsena42.floresta_node.R
import com.github.jvsena42.floresta_node.presentation.ui.screens.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess

class FlorestaService : Service() {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val florestaDaemon: FlorestaDaemon by inject()

    companion object {
        private const val TAG = "FlorestaService"
        private const val FLORESTA_NOTIFICATION_ID = 1000
        private const val CHANNEL_ID = "floresta_service_channel"
        private const val CHANNEL_NAME = "Floresta Service"

        const val ACTION_STOP_SERVICE = "com.github.jvsena42.floresta_node.ACTION_STOP_SERVICE"
        const val ACTION_EXIT_APP = "com.github.jvsena42.floresta_node.ACTION_EXIT_APP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(FLORESTA_NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Floresta node is running"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        // Intent to open the app when notification is clicked
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service and exit app
        val stopIntent = Intent(this, FlorestaService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floresta Node")
            .setContentText("Node is running in background")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_x,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stop service action received")
                stopServiceAndExitApp()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    ioScope.launch {
                        Log.d(TAG, "Starting Floresta daemon")
                        florestaDaemon.start()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onStartCommand error: ", e)
                }
            }
        }

        return START_STICKY
    }

    private fun stopServiceAndExitApp() {
        Log.d(TAG, "stopServiceAndExitApp called")

        // Immediately remove notification for instant feedback
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Notification removed")

        // Stop daemon and exit asynchronously
        ioScope.launch {
            try {
                Log.d(TAG, "Stopping Floresta daemon")
                florestaDaemon.stop()
                Log.d(TAG, "Floresta daemon stopped successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping daemon: ", e)
            }

            // Small delay to ensure daemon cleanup
            delay(500)

            // Send broadcast to close activities
            sendBroadcast(Intent(ACTION_EXIT_APP))

            // Stop service
            stopSelf()

            // Small delay before killing process
            delay(200)

            // Kill the app process
            Log.d(TAG, "Killing app process")
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        ioScope.launch {
            try {
                if (florestaDaemon.isRunning()) {
                    florestaDaemon.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onDestroy: ", e)
            }
        }
        ioScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}