package com.github.jvsena42.mandacaru.data.floresta

import android.util.Log
import com.florestad.AssumeUtreexoValue
import com.florestad.AssumeValidArg
import com.florestad.Config
import com.florestad.Florestad
import com.github.jvsena42.mandacaru.BuildConfig
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.model.Constants
import com.github.jvsena42.mandacaru.domain.settings.ELECTRUM_HOST
import com.github.jvsena42.mandacaru.domain.settings.electrumPortFor
import com.github.jvsena42.mandacaru.domain.settings.TorRoute
import com.github.jvsena42.mandacaru.domain.settings.torRoute
import com.github.jvsena42.mandacaru.presentation.utils.SnapshotCodec
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

import com.florestad.Network as FlorestaNetwork

class FlorestaDaemonImpl(
    private val datadir: String,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaDaemon {

    @Volatile private var isRunning = false
    private var daemon: Florestad? = null
    @Volatile private var runningDataDir: String? = null
    @Volatile private var runningTorRoute: TorRoute? = null

    // The service can deliver two start commands back to back; without this both pass the
    // isRunning check and the second start aborts the process inside the node.
    private val lifecycle = Mutex()

    @Volatile private var exiting = false

    override suspend fun start() {
        lifecycle.withLock { startLocked() }
    }

    private suspend fun startLocked() {
        if (isRunning || exiting) return
        runSuspendCatching {
            val pendingSnapshot = preferencesDataSource
                .getString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
                .takeIf { it.isNotEmpty() }
            val network = preferencesDataSource.getString(
                PreferenceKeys.CURRENT_NETWORK,
                FlorestaNetwork.BITCOIN.name
            ).toFlorestaNetwork()
            val filtersStartHeight = filtersStartHeightFor(network)
            val userAgent =
                "/Floresta:${Constants.FLORESTA_VERSION}/mandacaru:${BuildConfig.VERSION_NAME}/"
            val builtinSnapshotJson = builtinSnapshotFor(network)
            val startupSnapshotJson: String? = pendingSnapshot ?: builtinSnapshotJson
            val snapshotSource = when {
                pendingSnapshot != null -> "pending"
                builtinSnapshotJson != null -> "builtin"
                else -> "floresta-default"
            }
            val effectiveDataDir = dataDirFor(network)
            val rpcPort = rpcPortFor(network)
            val assumeUtreexoValue = startupSnapshotJson?.let { toAssumeUtreexoValue(it) }
            val torRoute = preferencesDataSource.torRoute()
            Log.i(
                TAG,
                "start: snapshotSource=$snapshotSource, " +
                    "snapshotApplied=${assumeUtreexoValue != null}, " +
                    "network=$network, datadir=$effectiveDataDir, rpcPort=$rpcPort, " +
                    "filtersStartHeight=$filtersStartHeight, " +
                    "proxy=${torRoute?.allPeersProxy}, onionProxy=${torRoute?.onionPeersProxy}, " +
                    "userAgent=$userAgent",
            )
            val config = Config(
                datadir = effectiveDataDir,
                network = network,
                assumeValid = AssumeValidArg.Hardcoded,
                proxy = torRoute?.allPeersProxy,
                onionProxy = torRoute?.onionPeersProxy,
                cfilters = true,
                filtersStartHeight = filtersStartHeight,
                jsonRpcAddress = "127.0.0.1:$rpcPort",
                electrumAddress = "$ELECTRUM_HOST:${preferencesDataSource.electrumPortFor(network)}",
                logToFile = true,
                assumeUtreexo = true,
                assumeutreexoValue = assumeUtreexoValue,
                userAgent = userAgent,
            )
            daemon = Florestad.fromConfig(config)
            // Published before the blocking start so requests authenticate as soon as the
            // node has written its cookie, not only once start() returns.
            runningDataDir = effectiveDataDir
            runningTorRoute = torRoute
            daemon?.start()
            Log.i(TAG, "start: Floresta running (snapshotSource=$snapshotSource)")
            isRunning = true
        }.onFailure { e ->
            Log.e(TAG, "start error: ", e)
            // runningDataDir stays: a start can fail after the node's RPC server is already
            // up (Electrum setup comes later), and clearing it would turn every request into
            // a 401 while that server still answers.
            isRunning = false
        }
    }

    private suspend fun filtersStartHeightFor(network: FlorestaNetwork): Int? {
        if (network != FlorestaNetwork.BITCOIN) return null
        val year = preferencesDataSource
            .getString(PreferenceKeys.WALLET_BIRTHDAY_YEAR, "")
            .toIntOrNull()
            ?: WalletBirthday.defaultYear()
        return WalletBirthday.bitcoinHeightForYear(year)
    }

    override suspend fun stop() {
        lifecycle.withLock { stopLocked() }
    }

    override suspend fun stopForExit() {
        lifecycle.withLock {
            exiting = true
            stopLocked()
        }
    }

    private fun stopLocked() {
        if (!isRunning) return
        try {
            daemon?.stop()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "stop error: ", e)
        } finally {
            isRunning = false
            daemon = null
            runningDataDir = null
            runningTorRoute = null
        }
    }

    override fun isRunning(): Boolean = isRunning

    override fun runningTorRoute(): TorRoute? = runningTorRoute

    override fun rpcCookieFile(): File? = runningDataDir?.let { File(it, RPC_COOKIE_FILE) }

    override suspend fun dumpUtreexoState(): Result<String> = withContext(Dispatchers.IO) {
        val d = daemon
        if (!isRunning || d == null) {
            return@withContext Result.failure(IllegalStateException("Daemon not running"))
        }
        runSuspendCatching { d.dumpUtreexoState() }
    }

    override suspend fun prepareForSnapshotImport(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRunning) {
            return@withContext Result.failure(
                IllegalStateException("Daemon is still running; call stop() first")
            )
        }
        val network = preferencesDataSource.getString(
            PreferenceKeys.CURRENT_NETWORK,
            FlorestaNetwork.BITCOIN.name
        ).toFlorestaNetwork()
        val base = File(dataDirFor(network))
        listOf("chaindata", "cfilter_headers.dat").forEach { sub ->
            val dir = File(base, sub)
            val size = if (dir.exists()) dirSize(dir) else 0L
            Log.i(TAG, "prepareForSnapshotImport: preserving $sub (size=$size)")
        }
        Result.success(Unit)
    }

    private fun dirSize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    // The builtin snapshot is a mainnet dump; Floresta rejects it on other networks,
    // so only apply it when running mainnet.
    private fun builtinSnapshotFor(network: FlorestaNetwork): String? {
        if (network != FlorestaNetwork.BITCOIN) return null
        return runCatching {
            SnapshotCodec.normalizeToJson(Constants.BUILTIN_UTREEXO_SNAPSHOT_COMPACT)
        }.onFailure {
            Log.w(TAG, "builtin snapshot decode failed; falling back to Floresta default", it)
        }.getOrNull()
    }

    private fun rpcPortFor(network: FlorestaNetwork): String = when (network) {
        FlorestaNetwork.BITCOIN -> Constants.RPC_PORT_MAINNET
        FlorestaNetwork.SIGNET -> Constants.RPC_PORT_SIGNET
        FlorestaNetwork.TESTNET -> Constants.RPC_PORT_TESTNET
        FlorestaNetwork.TESTNET4 -> Constants.RPC_PORT_TESTNET_4
        FlorestaNetwork.REGTEST -> Constants.RPC_PORT_REGTEST
    }

    // Decode a normalized snapshot JSON payload (network/block_hash/height/leaves/
    // roots) into the FFI's structured assume-utreexo value.
    private fun toAssumeUtreexoValue(json: String): AssumeUtreexoValue? = runCatching {
        val obj = JSONObject(json)
        val rootsArray = obj.getJSONArray("roots")
        val roots = List(rootsArray.length()) { rootsArray.getString(it) }
        AssumeUtreexoValue(
            blockHash = obj.getString("block_hash"),
            height = obj.getLong("height").toUInt(),
            roots = roots,
            leaves = obj.getLong("leaves").toULong(),
        )
    }.onFailure {
        Log.w(TAG, "failed to decode snapshot JSON to AssumeUtreexoValue", it)
    }.getOrNull()

    // Each network needs its own chain data. Mainnet keeps the flat base dir for
    // backward compatibility with already-synced installs; other networks live in a
    // dedicated subdir so they never collide with mainnet (or each other).
    private fun dataDirFor(network: FlorestaNetwork): String {
        if (network == FlorestaNetwork.BITCOIN) return datadir
        val dir = File(datadir, network.dirName())
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    private fun FlorestaNetwork.dirName(): String = when (this) {
        FlorestaNetwork.BITCOIN -> "bitcoin"
        FlorestaNetwork.SIGNET -> "signet"
        FlorestaNetwork.TESTNET -> "testnet"
        FlorestaNetwork.TESTNET4 -> "testnet4"
        FlorestaNetwork.REGTEST -> "regtest"
    }

    companion object {
        private const val TAG = "FlorestaDaemonImpl"
        private const val RPC_COOKIE_FILE = ".cookie"
    }
}
