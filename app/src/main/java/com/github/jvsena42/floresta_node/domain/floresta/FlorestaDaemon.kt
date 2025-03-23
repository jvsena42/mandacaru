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
}

class FlorestaDaemonImpl(
    private val datadir: String,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaDaemon {

    var isRunning = false
    private lateinit var daemon: Florestad
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
                    if (BuildConfig.DEBUG) FlorestaNetwork.SIGNET.name else FlorestaNetwork.BITCOIN.name
                ).toFlorestaNetwork(),
            )
            daemon = Florestad.fromConfig(config)
            daemon.start().also {
                Log.i(TAG, "start: Floresta running with config $config")
                isRunning = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "start error: ", e)
        }
    }

    override suspend fun stop() {
        if (!isRunning) return
        daemon.stop()
        isRunning = false
    }

    companion object {
        private const val TAG = "FlorestaDaemonImpl"
    }
}