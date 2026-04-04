package com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain

sealed interface BlockchainAction {
    data class OnSearchChanged(val query: String) : BlockchainAction
    data object ClearSnackBarMessage : BlockchainAction
    data object SearchLatestBlock : BlockchainAction
}
