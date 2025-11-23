package com.github.jvsena42.floresta_node.presentation.ui.screens.search

sealed interface SearchAction {
    data class OnSearchChanged(val transactionId: String): SearchAction
    data object ClearSnackBarMessage: SearchAction
}