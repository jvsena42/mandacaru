package com.github.jvsena42.floresta_node.domain.floresta

import android.util.Log
import com.florestad.Config
import com.florestad.Florestad
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.BuildConfig

import com.florestad.Network as FlorestaNetwork

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
}

class FlorestaDaemonImpl(
    private val datadir: String,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaDaemon {

    private var isRunning = false
    private var daemon: Florestad? = null

    override suspend fun start() {
        Log.d(TAG, "start: ")
        if (isRunning) {
            Log.d(TAG, "start: Daemon already running")
            return
        }
        try {
            Log.d(TAG, "start: datadir: $datadir")
            val config = Config(
                dataDir = datadir,
                electrumAddress = Constants.ELECTRUM_ADDRESS,
                network = preferencesDataSource.getString(
                    PreferenceKeys.CURRENT_NETWORK,
                    FlorestaNetwork.BITCOIN.name
                ).toFlorestaNetwork(),
            )
            daemon = Florestad.fromConfig(config)
            daemon?.start()?.also {
                Log.i(TAG, "start: Floresta running with config $config")
                isRunning = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "start error: ", e)
            isRunning = false
        }
    }

    override suspend fun stop() {
        Log.d(TAG, "stop: isRunning=$isRunning")
        if (!isRunning) {
            Log.d(TAG, "stop: Daemon not running, nothing to stop")
            return
        }

        try {
            daemon?.stop()
            Log.i(TAG, "stop: Floresta daemon stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "stop error: ", e)
        } finally {
            isRunning = false
            daemon = null
        }
    }

    override fun isRunning(): Boolean = isRunning

    companion object {
        private const val TAG = "FlorestaDaemonImpl"
    }
}