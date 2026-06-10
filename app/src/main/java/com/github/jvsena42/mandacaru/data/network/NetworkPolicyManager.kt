package com.github.jvsena42.mandacaru.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * Enforces the WiFi-only / "also use mobile data" policy process-wide.
 *
 * The Floresta daemon runs inside this app's own process, so its native TCP
 * sockets (and DNS lookups) follow [ConnectivityManager.bindProcessToNetwork].
 * Binding the process to a WiFi [Network] therefore keeps every peer connection
 * off cellular without any change to the Rust side. Loopback traffic
 * (the localhost JSON-RPC between UI and daemon) is routed over `lo` and stays
 * reachable regardless of the bound network.
 */
class NetworkPolicyManager(
    context: Context,
    private val preferencesDataSource: PreferencesDataSource,
) : NetworkPolicy {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    private val _isWaitingForWifi = MutableStateFlow(false)
    override val isWaitingForWifi: StateFlow<Boolean> = _isWaitingForWifi.asStateFlow()

    private var wifiCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Reads the current preference and (re)configures the binding. Safe to call
     * again after the preference changes; an app restart still happens on toggle
     * so existing daemon sockets are torn down, but re-applying keeps the
     * callback in a single, correct state.
     */
    override fun apply() {
        val allowMobileData = runBlocking {
            preferencesDataSource.getBoolean(PreferenceKeys.USE_ALSO_MOBILE_DATA, false)
        }
        unregisterWifiCallback()
        if (allowMobileData) {
            connectivityManager?.bindProcessToNetwork(null)
            _isWaitingForWifi.value = false
            Log.i(TAG, "apply: mobile data allowed — using system default network")
        } else {
            enforceWifiOnly()
        }
    }

    private fun enforceWifiOnly() {
        val cm = connectivityManager ?: return
        // Assume no WiFi until onAvailable confirms one, closing the startup
        // window before the callback fires.
        _isWaitingForWifi.value = true
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "WiFi available — binding process to it")
                cm.bindProcessToNetwork(network)
                _isWaitingForWifi.value = false
            }

            override fun onLost(network: Network) {
                // Do not rebind to null: that would let new sockets fall back to
                // cellular. Stay bound to the (now gone) WiFi network so sockets
                // simply fail until WiFi returns.
                Log.i(TAG, "WiFi lost — sync paused until WiFi returns")
                _isWaitingForWifi.value = true
            }
        }
        wifiCallback = callback
        cm.requestNetwork(request, callback)
        Log.i(TAG, "apply: WiFi-only enforced")
    }

    private fun unregisterWifiCallback() {
        wifiCallback?.let { cb ->
            runCatching { connectivityManager?.unregisterNetworkCallback(cb) }
        }
        wifiCallback = null
    }

    private companion object {
        const val TAG = "NetworkPolicyManager"
    }
}
