package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

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
import kotlin.time.Duration.Companion.milliseconds

class TransactionViewModel(
    private val florestaRpc: FlorestaRpc
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onAction(action: TransactionAction) {
        when (action) {
            is TransactionAction.OnSearchChanged -> {
                _uiState.update {
                    it.copy(transactionId = action.transactionId, errorMessage = "")
                }
                debouncedSearch()
            }
            TransactionAction.ClearSnackBarMessage -> {
                _uiState.update { it.copy(errorMessage = "") }
            }
            is TransactionAction.OnRawTxChanged -> {
                _uiState.update { it.copy(rawTxHex = action.rawTx, broadcastResult = "") }
            }
            TransactionAction.OnClickBroadcast -> broadcastTransaction()
        }
    }

    private fun debouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500.milliseconds)

            val txId = _uiState.value.transactionId.trim()

            if (txId.isEmpty()) {
                _uiState.update { it.copy(isSearchLoading = false, searchResult = null) }
                return@launch
            }

            if (!txId.matches(Regex("^[a-fA-F0-9]{64}$"))) {
                _uiState.update {
                    it.copy(isSearchLoading = false, errorMessage = "Invalid transaction ID format")
                }
                return@launch
            }

            _uiState.update { it.copy(isSearchLoading = true) }

            florestaRpc.getTransaction(txId).collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "getTransaction success: $data")
                    _uiState.update {
                        it.copy(searchResult = data, isSearchLoading = false)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "getTransaction error: ${error.message}")
                    if (error.message == "Transaction not found") {
                        florestaRpc.getBlockchainInfo().collect { infoResult ->
                            infoResult.onSuccess { info ->
                                val syncPercent =
                                    (info.result.progress * PERCENTAGE_MULTIPLIER).toInt()
                                val msg = if (info.result.ibd) {
                                    "Node is still syncing ($syncPercent%)." +
                                        " Transaction may appear once the relevant block is processed."
                                } else {
                                    "Transaction not found"
                                }
                                _uiState.update {
                                    it.copy(errorMessage = msg, isSearchLoading = false, searchResult = null)
                                }
                            }.onFailure {
                                _uiState.update {
                                    it.copy(errorMessage = "Transaction not found", isSearchLoading = false, searchResult = null)
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = error.message ?: "Failed to fetch transaction",
                                isSearchLoading = false,
                                searchResult = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun broadcastTransaction() {
        val hex = _uiState.value.rawTxHex.trim()
        if (hex.isEmpty()) return

        if (!hex.matches(Regex("^[a-fA-F0-9]+$"))) {
            _uiState.update { it.copy(errorMessage = "Invalid hex format") }
            return
        }

        _uiState.update { it.copy(isBroadcasting = true, broadcastResult = "") }

        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.sendRawTransaction(hex).collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "broadcast success: ${data.result}")
                    _uiState.update {
                        it.copy(
                            broadcastResult = data.result,
                            isBroadcasting = false,
                            rawTxHex = ""
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "broadcast error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Failed to broadcast transaction",
                            isBroadcasting = false
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    companion object {
        private const val TAG = "TransactionViewModel"
        private const val PERCENTAGE_MULTIPLIER = 100
    }
}
