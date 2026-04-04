package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

sealed interface TransactionAction {
    data class OnSearchChanged(val transactionId: String) : TransactionAction
    data object ClearSnackBarMessage : TransactionAction
    data class OnRawTxChanged(val rawTx: String) : TransactionAction
    data object OnClickBroadcast : TransactionAction
}
