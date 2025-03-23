package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.presentation.utils.toScientificNotationString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
            delay(3.seconds)
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
                            blockHeight = data.result.height.toString(),
                            difficulty = data.result.difficulty.toScientificNotationString(),
                            network = data.result.chain.uppercase(),
                            blockHash = data.result.bestBlock,
                            syncPercentage = (data.result.progress * 1000).roundToInt()
                        )
                    }
                }
            }
            florestaRpc.getPeerInfo().collect { result ->
                Log.d(TAG, "getPeerInfo: ${result.getOrNull()}")
                result.onSuccess { data ->
                    val peers = data.result.orEmpty()
                    if (peers.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                numberOfPeers = peers.size.toString(),
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "NodeViewModel"
    }
}