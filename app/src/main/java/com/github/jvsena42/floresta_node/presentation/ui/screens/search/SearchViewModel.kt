package com.github.jvsena42.floresta_node.presentation.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(
    private val florestaRpc: FlorestaRpc
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnSearchChanged -> {
                _uiState.update {
                    it.copy(
                        transactionId = action.transactionId,
                        errorMessage = ""
                    )
                }
                debouncedSearch()
            }

            SearchAction.ClearSnackBarMessage -> _uiState.update { it.copy(errorMessage = "") }
        }
    }

    private fun debouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500.milliseconds)

            val txId = _uiState.value.transactionId.trim()

            if (txId.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        searchResult = null
                    )
                }
                return@launch
            }

            // Validate transaction ID format (64 hex characters)
            if (!txId.matches(Regex("^[a-fA-F0-9]{64}$"))) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid transaction ID format"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            florestaRpc.getTransaction(txId).collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "getTransaction success: $data")
                    _uiState.update {
                        it.copy(
                            searchResult = data,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "getTransaction error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Failed to fetch transaction",
                            isLoading = false,
                            searchResult = null
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
        private const val TAG = "SearchViewModel"
    }
}