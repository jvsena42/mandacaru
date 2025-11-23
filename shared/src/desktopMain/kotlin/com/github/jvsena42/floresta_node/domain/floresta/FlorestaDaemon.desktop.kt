package com.github.jvsena42.floresta_node.domain.floresta

import com.florestad.Config
import com.florestad.Florestad
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.platform.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.florestad.Network as FlorestaNetwork

actual fun createFlorestaDaemon(
    datadir: String,
    network: String
): FlorestaDaemon {
    return DesktopFlorestaDaemon(datadir, network)
}

class DesktopFlorestaDaemon(
    private val datadir: String,
    private val networkName: String
) : FlorestaDaemon {

    private var isRunning = false
    private var daemon: Florestad? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun start() {
        platformLog(TAG, "start: ")
        if (isRunning) {
            platformLog(TAG, "start: Daemon already running")
            return
        }
        try {
            platformLog(TAG, "start: datadir: $datadir")
            val config = Config(
                dataDir = datadir,
                electrumAddress = Constants.ELECTRUM_ADDRESS,
                network = networkName.toFlorestaNetwork(),
            )
            daemon = Florestad.fromConfig(config)

            // Start daemon in background coroutine
            scope.launch {
                daemon?.start()?.also {
                    platformLog(TAG, "start: Floresta running with config $config")
                    isRunning = true
                }
            }
        } catch (e: Exception) {
            platformLog(TAG, "start error: ${e.message}")
            isRunning = false
        }
    }

    override suspend fun stop() {
        platformLog(TAG, "stop: isRunning=$isRunning")
        if (!isRunning) {
            platformLog(TAG, "stop: Daemon not running, nothing to stop")
            return
        }

        try {
            daemon?.stop()
            platformLog(TAG, "stop: Floresta daemon stopped successfully")
        } catch (e: Exception) {
            platformLog(TAG, "stop error: ${e.message}")
        } finally {
            isRunning = false
            daemon = null
        }
    }

    override fun isRunning(): Boolean = isRunning

    private fun String.toFlorestaNetwork(): FlorestaNetwork {
        return when (this.uppercase()) {
            "BITCOIN" -> FlorestaNetwork.BITCOIN
            "TESTNET" -> FlorestaNetwork.TESTNET
            "SIGNET" -> FlorestaNetwork.SIGNET
            "REGTEST" -> FlorestaNetwork.REGTEST
            else -> FlorestaNetwork.BITCOIN
        }
    }

    companion object {
        private const val TAG = "DesktopFlorestaDaemon"
    }
}
