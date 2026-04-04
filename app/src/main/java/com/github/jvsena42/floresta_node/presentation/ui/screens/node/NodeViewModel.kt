package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.presentation.utils.toHumanReadableDifficulty
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
                            syncPercentage = "%.2f".format(data.result.progress * 100),
                            syncDecimal = data.result.progress,
                            validatedBLocks = data.result.validated
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
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${secs}s")
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
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
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "NodeViewModel"
    }
}