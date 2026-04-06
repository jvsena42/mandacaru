package com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BlockchainViewModel(
    private val florestaRpc: FlorestaRpc
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockchainUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshChainStatus()
    }

    private fun refreshChainStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            updateChainStatus()
            delay(30.seconds)
            refreshChainStatus()
        }
    }

    private suspend fun updateChainStatus() {
        florestaRpc.getBlockchainInfo().collect { result ->
            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        blockCount = NumberFormat.getNumberInstance().format(data.result.height),
                        bestBlockHash = data.result.bestBlock,
                        validatedBlocks = data.result.validated
                    )
                }
            }
        }
    }

    fun onAction(action: BlockchainAction) {
        when (action) {
            is BlockchainAction.OnSearchChanged -> {
                _uiState.update {
                    it.copy(searchQuery = action.query, errorMessage = "")
                }
                debouncedSearch()
            }
            BlockchainAction.ClearSnackBarMessage -> {
                _uiState.update { it.copy(errorMessage = "") }
            }
            BlockchainAction.SearchLatestBlock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    searchByHash(_uiState.value.bestBlockHash)
                }
            }
        }
    }

    private fun debouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500.milliseconds)

            val query = _uiState.value.searchQuery.trim()

            if (query.isEmpty()) {
                _uiState.update {
                    it.copy(isLoading = false, blockHeader = null, blockHash = "", blockHeight = "")
                }
                return@launch
            }

            val height = query.toIntOrNull()
            if (height != null) {
                searchByHeight(height)
            } else if (query.matches(Regex("^[a-fA-F0-9]{64}$"))) {
                searchByHash(query)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Enter a block height (number) or block hash (64 hex characters)"
                    )
                }
            }
        }
    }

    private suspend fun searchByHeight(height: Int) {
        _uiState.update { it.copy(isLoading = true) }

        florestaRpc.getBlockHash(height).collect { result ->
            result.onSuccess { data ->
                _uiState.update { it.copy(blockHash = data.result, blockHeight = height.toString()) }
                fetchBlockHeader(data.result)
            }.onFailure { error ->
                Log.e(TAG, "getBlockHash error: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Block not found",
                        blockHeader = null
                    )
                }
            }
        }
    }

    private suspend fun searchByHash(hash: String) {
        _uiState.update { it.copy(isLoading = true, searchQuery = hash) }
        _uiState.update { it.copy(blockHash = hash, blockHeight = "") }
        fetchBlockHeader(hash)
    }

    private suspend fun fetchBlockHeader(hash: String) {
        florestaRpc.getBlockHeader(hash).collect { result ->
            result.onSuccess { data ->
                Log.d(TAG, "getBlockHeader success: $data")
                _uiState.update {
                    it.copy(
                        blockHeader = data.result,
                        isLoading = false,
                        blockHash = hash
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "getBlockHeader error: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to fetch block header",
                        blockHeader = null
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    companion object {
        private const val TAG = "BlockchainViewModel"
        private const val MILLIS_PER_SECOND = 1000

        fun formatBlockTime(timestamp: Long): String {
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(timestamp * MILLIS_PER_SECOND))
        }
    }
}
