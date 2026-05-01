package com.github.jvsena42.mandacaru.data.floresta

import android.util.Log
import com.florestad.Config
import com.florestad.Florestad
import com.github.jvsena42.mandacaru.BuildConfig
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.model.Constants
import com.github.jvsena42.mandacaru.presentation.utils.SnapshotCodec
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday
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
            val pendingSnapshot = preferencesDataSource
                .getString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
                .takeIf { it.isNotEmpty() }
            val network = preferencesDataSource.getString(
                PreferenceKeys.CURRENT_NETWORK,
                FlorestaNetwork.BITCOIN.name
            ).toFlorestaNetwork()
            val filtersStartHeight = if (network == FlorestaNetwork.BITCOIN) {
                val year = preferencesDataSource
                    .getString(PreferenceKeys.WALLET_BIRTHDAY_YEAR, "")
                    .toIntOrNull()
                    ?: WalletBirthday.defaultYear()
                WalletBirthday.bitcoinHeightForYear(year)
            } else null
            val userAgent =
                "/Floresta:${Constants.FLORESTA_VERSION}/mandacaru:${BuildConfig.VERSION_NAME}/"
            val builtinSnapshotJson: String? = runCatching {
                SnapshotCodec.normalizeToJson(Constants.BUILTIN_UTREEXO_SNAPSHOT_COMPACT)
            }.onFailure {
                Log.w(TAG, "builtin snapshot decode failed; falling back to Floresta default", it)
            }.getOrNull()
            val startupSnapshotJson: String? = pendingSnapshot ?: builtinSnapshotJson
            val snapshotSource = when {
                pendingSnapshot != null -> "pending"
                builtinSnapshotJson != null -> "builtin"
                else -> "floresta-default"
            }
            Log.i(
                TAG,
                "start: snapshotSource=$snapshotSource, " +
                    "snapshotLen=${startupSnapshotJson?.length ?: 0}, " +
                    "network=$network, datadir=$datadir, " +
                    "filtersStartHeight=$filtersStartHeight, userAgent=$userAgent",
            )
            val config = Config(
                dataDir = datadir,
                network = network,
                assumeUtreexo = true,
                userUtreexoSnapshotJson = startupSnapshotJson,
                filtersStartHeight = filtersStartHeight,
                userAgent = userAgent,
            )
            daemon = Florestad.fromConfig(config)
            daemon?.start()?.also {
                Log.i(
                    TAG,
                    "start: Floresta running (snapshotSource=$snapshotSource)",
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
