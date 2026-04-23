package com.github.jvsena42.mandacaru.data.floresta

import android.util.Log
import com.florestad.Config
import com.florestad.Florestad
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import com.florestad.Network as FlorestaNetwork

class FlorestaDaemonImpl(
    private val datadir: String,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaDaemon {

    private var isRunning = false
    private var daemon: Florestad? = null

    override suspend fun start() {
        if (isRunning) return
        try {
            val fastSyncEnabled = preferencesDataSource.getBoolean(
                PreferenceKeys.FAST_SYNC_ENABLED,
                false
            )
            val pendingSnapshot = preferencesDataSource
                .getString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
                .takeIf { it.isNotEmpty() }
            val network = preferencesDataSource.getString(
                PreferenceKeys.CURRENT_NETWORK,
                FlorestaNetwork.BITCOIN.name
            ).toFlorestaNetwork()
            val effectiveAssumeUtreexo = fastSyncEnabled || pendingSnapshot != null
            Log.i(
                TAG,
                "start: pendingSnapshot=${pendingSnapshot?.length ?: 0} chars, " +
                    "fastSync=$fastSyncEnabled→$effectiveAssumeUtreexo, " +
                    "network=$network, datadir=$datadir",
            )
            val config = Config(
                dataDir = datadir,
                network = network,
                assumeUtreexo = effectiveAssumeUtreexo,
                userUtreexoSnapshotJson = pendingSnapshot,
            )
            daemon = Florestad.fromConfig(config)
            daemon?.start()?.also {
                Log.i(
                    TAG,
                    "start: Floresta running (pendingSnapshot=${pendingSnapshot != null}, " +
                        "assumeUtreexo=$effectiveAssumeUtreexo)",
                )
                isRunning = true
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "start error: ", e)
            isRunning = false
        }
    }

    override suspend fun stop() {
        if (!isRunning) return
        try {
            daemon?.stop()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "stop error: ", e)
        } finally {
            isRunning = false
            daemon = null
        }
    }

    override fun isRunning(): Boolean = isRunning

    override suspend fun dumpUtreexoState(): Result<String> = withContext(Dispatchers.IO) {
        val d = daemon
        if (!isRunning || d == null) {
            return@withContext Result.failure(IllegalStateException("Daemon not running"))
        }
        runCatching { d.dumpUtreexoState() }
    }

    override suspend fun prepareForSnapshotImport(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRunning) {
            return@withContext Result.failure(
                IllegalStateException("Daemon is still running; call stop() first")
            )
        }
        val base = File(datadir)
        listOf("chaindata", "cfilters").forEach { sub ->
            val dir = File(base, sub)
            val size = if (dir.exists()) dirSize(dir) else 0L
            Log.i(TAG, "prepareForSnapshotImport: preserving $sub (size=$size)")
        }
        Result.success(Unit)
    }

    private fun dirSize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    companion object {
        private const val TAG = "FlorestaDaemonImpl"
    }
}
