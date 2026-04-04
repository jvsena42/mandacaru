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
import androidx.core.graphics.toColorInt
import com.github.jvsena42.floresta_node.R
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.presentation.ui.screens.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicBoolean

class FlorestaService : Service() {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val florestaDaemon: FlorestaDaemon by inject()
    private val florestaRpc: FlorestaRpc by inject()
    private val isStopping = AtomicBoolean(false)
    private var notificationPollingJob: Job? = null

    companion object {
        private const val TAG = "FlorestaService"
        private const val FLORESTA_NOTIFICATION_ID = 1000
        private const val CHANNEL_ID = "floresta_service_channel"
        private const val CHANNEL_NAME = "Floresta Service"

        const val ACTION_STOP_SERVICE = "com.github.jvsena42.floresta_node.ACTION_STOP_SERVICE"
        const val ACTION_EXIT_APP = "com.github.jvsena42.floresta_node.ACTION_EXIT_APP"
        private const val STOP_TIMEOUT_MS = 10_000L
        private const val NOTIFICATION_POLL_INTERVAL_MS = 10_000L
        private const val COLOR_PRIMARY = "#FF815600"
        private const val COLOR_SYNCED = "#006D37"
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
            .setContentText("Starting node...")
            .setSubText("Initializing")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setColor(COLOR_PRIMARY.toColorInt())
            .setColorized(true)
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
                    startNotificationPolling()
                } catch (e: Exception) {
                    Log.e(TAG, "onStartCommand error: ", e)
                }
            }
        }

        return START_STICKY
    }

    private fun startNotificationPolling() {
        notificationPollingJob?.cancel()
        notificationPollingJob = ioScope.launch {
            while (true) {
                delay(NOTIFICATION_POLL_INTERVAL_MS)
                try {
                    florestaRpc.getBlockchainInfo().collect { result ->
                        result.onSuccess { data ->
                            updateSyncNotification(data.result.progress, data.result.height)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Notification poll failed: ${e.message}")
                }
            }
        }
    }

    private fun updateSyncNotification(progress: Float, height: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FlorestaService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floresta Node")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_x, "Stop", stopPendingIntent)

        if (progress < 1.0f) {
            val percentage = progress * 100
            builder.setContentText("Syncing: ${"%.2f".format(percentage)}%")
                .setSubText("${"%.2f".format(percentage)}% complete")
                .setProgress(100, percentage.toInt(), false)
                .setColor(COLOR_PRIMARY.toColorInt())
                .setColorized(true)
        } else {
            val formattedHeight = NumberFormat.getNumberInstance().format(height)
            builder.setContentText("Synced - Block #$formattedHeight")
                .setSubText("Fully synced")
                .setColor(COLOR_SYNCED.toColorInt())
                .setColorized(true)
        }

        notificationManager.notify(FLORESTA_NOTIFICATION_ID, builder.build())
    }

    private fun stopServiceAndExitApp() {
        if (!isStopping.compareAndSet(false, true)) {
            Log.d(TAG, "stopServiceAndExitApp: already stopping, ignoring")
            return
        }
        Log.d(TAG, "stopServiceAndExitApp called")
        notificationPollingJob?.cancel()

        // Update notification to show shutdown in progress
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(FLORESTA_NOTIFICATION_ID, createStoppingNotification())

        ioScope.launch {
            stopDaemonWithTimeout()

            // Send broadcast to close activities
            sendBroadcast(Intent(ACTION_EXIT_APP))

            // Stop foreground service and remove notification
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun stopDaemonWithTimeout() {
        Log.d(TAG, "Stopping Floresta daemon")
        runCatching {
            withTimeoutOrNull(STOP_TIMEOUT_MS) {
                florestaDaemon.stop()
            }
        }.onSuccess { result ->
            if (result == null) {
                Log.w(TAG, "Floresta daemon stop timed out after ${STOP_TIMEOUT_MS}ms")
            } else {
                Log.d(TAG, "Floresta daemon stopped successfully")
            }
        }.onFailure { e ->
            Log.e(TAG, "Error stopping daemon: ", e)
        }
    }

    private fun createStoppingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floresta Node")
            .setContentText("Stopping node...")
            .setSubText("Shutting down")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setColor(COLOR_PRIMARY.toColorInt())
            .setColorized(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        notificationPollingJob?.cancel()
        if (isStopping.compareAndSet(false, true)) {
            // Only stop daemon here if stopServiceAndExitApp wasn't called
            runBlocking(Dispatchers.IO) {
                stopDaemonWithTimeout()
            }
        }
        ioScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}