package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

sealed interface TransactionAction {
    data class OnSearchChanged(val transactionId: String) : TransactionAction
    data object ClearSnackBarMessage : TransactionAction
    data object ClearBroadcastResult : TransactionAction
    data class OnRawTxChanged(val rawTx: String) : TransactionAction
    data object OnClickBroadcast : TransactionAction
    data object OnClickScan : TransactionAction
    data object OnDismissScanner : TransactionAction
    data class OnQrFrameScanned(val payload: String) : TransactionAction
    data class OnScanPasteSubmitted(val text: String) : TransactionAction
    data object OnConfirmBroadcast : TransactionAction
    data object OnDismissConfirmation : TransactionAction
}
