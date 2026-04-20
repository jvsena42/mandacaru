package com.github.jvsena42.mandacaru.domain.floresta

import android.util.Log
import com.florestad.Network as FlorestaNetwork
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.Constants
import kotlinx.coroutines.flow.firstOrNull

// When an IBD node has zero Utreexo-flagged peers it cannot make progress, because inclusion
// proofs are served only by those peers. Floresta's bundled seed file has only ~2 live Utreexo
// bridges on mainnet, so on a clean install discovery can fail for hours. This class calls
// `addnode` against the known-good bridges at startup and whenever the Utreexo peer count
// drops to zero, with a throttle so we don't spam the RPC every poll cycle.
class UtreexoBridgeAutoConnect(
    private val florestaRpc: FlorestaRpc,
    private val preferencesDataSource: PreferencesDataSource,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private var lastAttemptAtMs: Long? = null

    suspend fun seedOnStartup() {
        val bridges = currentBridges()
        if (bridges.isEmpty()) {
            Log.d(TAG, "seedOnStartup: no bridges configured for current network")
            return
        }
        // Claim the throttle window BEFORE the addnode calls so a concurrent poll
        // doesn't re-fire while these are still in flight (addnode can take tens of
        // seconds per bridge when the host is slow to respond).
        lastAttemptAtMs = nowMs()
        Log.i(TAG, "seedOnStartup: adding ${bridges.size} bridge(s)")
        addBridges(bridges)
    }

    suspend fun ensureUtreexoPeers() {
        val bridges = currentBridges()
        if (bridges.isEmpty()) return

        val now = nowMs()
        if (hasConnectedUtreexoPeer() || isWithinThrottleWindow(now)) return

        lastAttemptAtMs = now
        Log.i(TAG, "ensureUtreexoPeers: no Utreexo peers, re-adding ${bridges.size} bridge(s)")
        addBridges(bridges)
    }

    private suspend fun hasConnectedUtreexoPeer(): Boolean =
        florestaRpc.getPeerInfo().firstOrNull()
            ?.getOrNull()
            ?.result
            .orEmpty()
            .any { it.services.hasUtreexoServiceFlag() }

    private fun isWithinThrottleWindow(now: Long): Boolean {
        val last = lastAttemptAtMs ?: return false
        val withinWindow = now - last < RETRY_THROTTLE_MS
        if (withinWindow) Log.d(TAG, "ensureUtreexoPeers: within throttle window, skipping")
        return withinWindow
    }

    private suspend fun currentBridges(): List<String> {
        val network = preferencesDataSource.getString(
            PreferenceKeys.CURRENT_NETWORK,
            FlorestaNetwork.BITCOIN.name,
        )
        return Constants.utreexoBridgesFor(network)
    }

    private suspend fun addBridges(bridges: List<String>) {
        bridges.forEach { bridge ->
            // "onetry" forces an immediate outbound connection attempt; "add" registers the
            // address in the persistent pool so Floresta retries on its own if the link drops.
            // Without "onetry" the Rust daemon often accepts "add" without ever dialling the
            // address
            addBridgeWith(bridge, command = "onetry")
            addBridgeWith(bridge, command = "add")
        }
    }

    private suspend fun addBridgeWith(bridge: String, command: String) {
        val result = florestaRpc.addNode(bridge, command).firstOrNull()
        result?.onSuccess { Log.d(TAG, "addNode($bridge, $command) ok") }
        result?.onFailure { Log.w(TAG, "addNode($bridge, $command) failed: ${it.message}") }
    }

    private companion object {
        const val TAG = "UtreexoBridgeAutoConnect"
        const val RETRY_THROTTLE_MS = 60_000L
    }
}
