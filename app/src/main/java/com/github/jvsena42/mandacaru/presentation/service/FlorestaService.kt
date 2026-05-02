package com.github.jvsena42.mandacaru.presentation.service

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
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoBridgeAutoConnect
import com.github.jvsena42.mandacaru.domain.floresta.computeHeaderSyncProgress
import com.github.jvsena42.mandacaru.domain.floresta.isLikelyStalled
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import com.github.jvsena42.mandacaru.presentation.ui.screens.main.MainActivity
import com.github.jvsena42.mandacaru.presentation.utils.toSyncPercentageString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
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
    private val utreexoBridgeAutoConnect: UtreexoBridgeAutoConnect by inject()
    private val preferencesDataSource: PreferencesDataSource by inject()
    private val isStopping = AtomicBoolean(false)
    private var notificationPollingJob: Job? = null
    private var startupSeedDone = false
    private var fullySyncedSinceMs: Long? = null
    private val rescanInFlight = AtomicBoolean(false)

    companion object {
        private const val TAG = "FlorestaService"
        private const val FLORESTA_NOTIFICATION_ID = 1000
        private const val CHANNEL_ID = "floresta_service_channel"
        private const val CHANNEL_NAME = "Mandacaru Service"

        const val ACTION_STOP_SERVICE = "com.github.jvsena42.mandacaru.ACTION_STOP_SERVICE"
        const val ACTION_EXIT_APP = "com.github.jvsena42.mandacaru.ACTION_EXIT_APP"
        private const val STOP_TIMEOUT_MS = 10_000L
        private const val NOTIFICATION_POLL_INTERVAL_MS = 10_000L
        private const val COLOR_PRIMARY = "#FF815600"
        private const val COLOR_SYNCED = "#006D37"
        private const val FULL_SYNC_THRESHOLD = 1.0f
        private const val PERCENTAGE_MULTIPLIER = 100
        // Filter sync usually catches up to chain sync within seconds, but
        // matched-block downloads add tail latency. Wait this long after the
        // chain reports fully synced before triggering the descriptor rescan.
        private const val FULL_SYNC_GRACE_MS = 60_000L
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
            description = "Shows when Mandacaru is running"
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
            .setContentTitle("Mandacaru")
            .setContentText("Starting node...")
            .setSubText("Initializing")
            .setSmallIcon(R.drawable.ic_notification)
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
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
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
                            val peers = florestaRpc.getPeerInfo().firstOrNull()
                                ?.getOrNull()?.result.orEmpty()
                            updateSyncNotification(
                                progress = data.result.progress,
                                height = data.result.height,
                                ibd = data.result.ibd,
                                peers = peers,
                            )
                            if (!startupSeedDone) {
                                startupSeedDone = true
                                launch { utreexoBridgeAutoConnect.seedOnStartup() }
                            } else {
                                launch { utreexoBridgeAutoConnect.ensureUtreexoPeers() }
                            }
                            val stalled = isLikelyStalled(
                                progress = data.result.progress,
                                ibd = data.result.ibd,
                                ourHeight = data.result.height,
                                peers = peers,
                            )
                            maybeTriggerWalletRescan(
                                progress = data.result.progress,
                                ibd = data.result.ibd,
                                stalled = stalled,
                            )
                        }
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Log.d(TAG, "Notification poll failed: ${e.message}")
                }
            }
        }
    }

    /**
     * After a wallet-birthday change, Floresta wipes the compact filter store
     * and re-syncs from the new height — but the wallet's cached descriptors
     * are not auto-rescanned against the new store. Trigger a rescan once the
     * chain looks fully synced (with a grace window so filter downloads can
     * catch up too), then clear the flag.
     */
    private fun maybeTriggerWalletRescan(progress: Float, ibd: Boolean, stalled: Boolean) {
        if (ibd || progress < FULL_SYNC_THRESHOLD || stalled) {
            fullySyncedSinceMs = null
            return
        }
        val now = System.currentTimeMillis()
        val syncedSince = fullySyncedSinceMs ?: now.also { fullySyncedSinceMs = it }
        if (now - syncedSince < FULL_SYNC_GRACE_MS) return
        if (!rescanInFlight.compareAndSet(false, true)) return

        ioScope.launch {
            try {
                val needsRescan = preferencesDataSource
                    .getBoolean(PreferenceKeys.WALLET_NEEDS_RESCAN, false)
                if (!needsRescan) return@launch

                Log.i(TAG, "Triggering rescanblockchain after wallet-birthday change")
                val result = florestaRpc.rescan().firstOrNull()
                if (result?.isSuccess == true) {
                    preferencesDataSource.setBoolean(PreferenceKeys.WALLET_NEEDS_RESCAN, false)
                    Log.i(TAG, "Wallet rescan triggered; flag cleared")
                } else {
                    Log.w(TAG, "rescan failed: ${result?.exceptionOrNull()?.message}")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.w(TAG, "Wallet rescan trigger failed: ${e.message}")
            } finally {
                rescanInFlight.set(false)
            }
        }
    }

    private fun updateSyncNotification(
        progress: Float,
        height: Int,
        ibd: Boolean,
        peers: List<PeerInfoResult>,
    ) {
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
            .setContentTitle("Mandacaru")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_x, "Stop", stopPendingIntent)

        val isHeaderSync = ibd && progress == 0f
        val headerDecimal = if (isHeaderSync) {
            computeHeaderSyncProgress(height, peers)
        } else {
            null
        }
        val stalled = isLikelyStalled(
            progress = progress,
            ibd = ibd,
            ourHeight = height,
            peers = peers,
        )

        when {
            headerDecimal != null -> {
                val label = headerDecimal.toSyncPercentageString()
                val barProgress = (headerDecimal * PERCENTAGE_MULTIPLIER).toInt()
                builder.setContentText("Syncing headers: $label%")
                    .setSubText("$label% headers")
                    .setProgress(PERCENTAGE_MULTIPLIER, barProgress, false)
                    .setColor(COLOR_PRIMARY.toColorInt())
                    .setColorized(true)
            }
            isHeaderSync -> {
                builder.setContentText("Syncing headers…")
                    .setSubText("Connecting to peers")
                    .setProgress(0, 0, true)
                    .setColor(COLOR_PRIMARY.toColorInt())
                    .setColorized(true)
            }
            stalled -> {
                val formattedHeight = NumberFormat.getNumberInstance().format(height)
                builder.setContentText("Sync stalled at block #$formattedHeight")
                    .setSubText("Storage may be unhealthy")
                    .setColor(COLOR_PRIMARY.toColorInt())
                    .setColorized(true)
            }
            progress < FULL_SYNC_THRESHOLD -> {
                val label = progress.toSyncPercentageString()
                val barProgress = (progress * PERCENTAGE_MULTIPLIER).toInt()
                builder.setContentText("Syncing blocks: $label%")
                    .setSubText("$label% blocks")
                    .setProgress(PERCENTAGE_MULTIPLIER, barProgress, false)
                    .setColor(COLOR_PRIMARY.toColorInt())
                    .setColorized(true)
            }
            else -> {
                val formattedHeight = NumberFormat.getNumberInstance().format(height)
                builder.setContentText("Synced - Block #$formattedHeight")
                    .setSubText("Fully synced")
                    .setColor(COLOR_SYNCED.toColorInt())
                    .setColorized(true)
            }
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
            .setContentTitle("Mandacaru")
            .setContentText("Stopping node...")
            .setSubText("Shutting down")
            .setSmallIcon(R.drawable.ic_notification)
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
