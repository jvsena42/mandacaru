package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.domain.floresta.hasUtreexoServiceFlag
import com.github.jvsena42.mandacaru.presentation.utils.toHumanReadableDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds

class NodeViewModel(
    private val florestaRpc: FlorestaRpc
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getInLoop()
    }

    private fun getInLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            getInfo()
            delay(10.seconds)
            getInLoop()
        }
    }

    private fun getInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.getBlockchainInfo().collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "getBlockchainInfo: $data")
                    _uiState.update {
                        it.copy(
                            blockHeight = NumberFormat.getNumberInstance().format(data.result.height),
                            difficulty = data.result.difficulty.toHumanReadableDifficulty(),
                            network = data.result.chain.uppercase(),
                            blockHash = data.result.bestBlock,
                            syncPercentage = "%.2f".format(data.result.progress * PERCENTAGE_MULTIPLIER),
                            syncDecimal = data.result.progress,
                            validatedBLocks = data.result.validated,
                            ibd = data.result.ibd
                        )
                    }
                }
                updatePeerInfo()
                updateDiagnostics()
            }
        }
    }

    fun togglePeersExpanded() {
        _uiState.update { it.copy(isPeersExpanded = !it.isPeersExpanded) }
    }

    fun toggleDiagnosticsExpanded() {
        _uiState.update { it.copy(isDiagnosticsExpanded = !it.isDiagnosticsExpanded) }
    }

    private suspend fun updateDiagnostics() {
        florestaRpc.getUptime().collect { result ->
            result.onSuccess { data ->
                _uiState.update { it.copy(uptime = formatUptime(data.result)) }
            }
        }
        florestaRpc.getMemoryInfo().collect { result ->
            result.onSuccess { data ->
                val locked = data.result.locked
                _uiState.update {
                    it.copy(
                        memoryUsed = formatBytes(locked.used),
                        memoryFree = formatBytes(locked.free),
                        memoryTotal = formatBytes(locked.total),
                    )
                }
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val days = seconds / SECONDS_PER_DAY
        val hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR
        val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${secs}s")
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= BYTES_PER_MB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
            bytes >= BYTES_PER_KB -> "%.1f KB".format(bytes / BYTES_PER_KB.toDouble())
            else -> "$bytes B"
        }
    }

    fun disconnectPeer(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.disconnectNode(address).collect { result ->
                result.onSuccess {
                    Log.d(TAG, "disconnectPeer success: $address")
                    updatePeerInfo()
                }
                result.onFailure { e ->
                    Log.e(TAG, "disconnectPeer failure: ${e.message}")
                }
            }
        }
    }

    fun pingPeers() {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.ping().collect { result ->
                result.onSuccess { Log.d(TAG, "ping success") }
                result.onFailure { e -> Log.e(TAG, "ping failure: ${e.message}") }
            }
        }
    }

    private suspend fun updatePeerInfo() {
        florestaRpc.getPeerInfo().collect { result ->
            Log.d(TAG, "getPeerInfo: ${result.getOrNull()}")
            result.onSuccess { data ->
                val peers = data.result.orEmpty()
                _uiState.update {
                    it.copy(
                        numberOfPeers = peers.size.toString(),
                        peers = peers,
                        utreexoPeerCount = peers.count { p -> p.services.hasUtreexoServiceFlag() },
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "NodeViewModel"
        const val PERCENTAGE_MULTIPLIER = 100
        const val SECONDS_PER_DAY = 86400L
        const val SECONDS_PER_HOUR = 3600L
        const val SECONDS_PER_MINUTE = 60L
        const val BYTES_PER_KB = 1_024L
        const val BYTES_PER_MB = 1_048_576L
    }
}
