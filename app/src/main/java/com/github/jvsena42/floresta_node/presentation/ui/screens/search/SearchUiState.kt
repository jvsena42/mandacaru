package com.github.jvsena42.floresta_node.presentation.ui.screens.search

import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse

data class SearchUiState(
    val transactionId: String = "",
    val searchResult: GetTransactionResponse? = null,
    val errorMessage: String = "",
    val isLoading: Boolean = false
)