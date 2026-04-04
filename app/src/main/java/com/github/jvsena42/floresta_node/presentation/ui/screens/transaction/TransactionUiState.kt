package com.github.jvsena42.floresta_node.presentation.ui.screens.transaction

import androidx.compose.runtime.Stable
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse

@Stable
data class TransactionUiState(
    val transactionId: String = "",
    val searchResult: GetTransactionResponse? = null,
    val isSearchLoading: Boolean = false,
    val rawTxHex: String = "",
    val broadcastResult: String = "",
    val isBroadcasting: Boolean = false,
    val errorMessage: String = "",
)
