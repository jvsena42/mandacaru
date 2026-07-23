package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.annotation.StringRes
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.floresta.SyncPhase
import com.github.jvsena42.mandacaru.domain.floresta.SyncSnapshot

fun NodeUiState.toSyncSnapshot() = SyncSnapshot(
    ibd = ibd,
    progress = syncDecimal,
    filterSyncDecimal = filterSyncDecimal,
    stalled = isStalled,
    rescanInProgress = rescanInProgress,
    rescanPending = walletRescanPending,
)

@StringRes
fun SyncPhase.titleRes(): Int = when (this) {
    SyncPhase.HEADERS -> R.string.syncing_headers_title
    SyncPhase.STALLED -> R.string.sync_stalled_title
    SyncPhase.BLOCKS -> R.string.syncing_blocks_title
    SyncPhase.FILTERS -> R.string.syncing_filters_title
    SyncPhase.WALLET_SCAN -> R.string.scanning_wallet_title
    SyncPhase.SYNCED -> R.string.sync
}

internal data class SyncStepStates(
    val headers: StepState,
    val blocks: StepState,
    val filters: StepState?,
    val allDone: Boolean,
)

private fun blocksStepState(
    headersDone: Boolean,
    blocksDone: Boolean,
    walletScanning: Boolean,
    hasFiltersStep: Boolean,
): StepState = when {
    walletScanning && !hasFiltersStep -> StepState.Current
    blocksDone -> StepState.Done
    headersDone -> StepState.Current
    else -> StepState.Pending
}

private fun filtersStepState(
    hasFiltersStep: Boolean,
    walletScanning: Boolean,
    filtersDownloaded: Boolean,
    blocksDone: Boolean,
): StepState? = when {
    !hasFiltersStep -> null
    walletScanning -> StepState.Current
    filtersDownloaded && blocksDone -> StepState.Done
    blocksDone -> StepState.Current
    else -> StepState.Pending
}

internal fun computeSyncStepStates(
    phase: SyncPhase,
    syncDecimal: Float,
    filterSyncDecimal: Float?,
): SyncStepStates {
    val hasFiltersStep = filterSyncDecimal != null
    val headersDone = phase != SyncPhase.HEADERS
    val blocksDone = syncDecimal >= COMPLETE
    val filtersDownloaded = filterSyncDecimal == null || filterSyncDecimal >= COMPLETE
    // A rescan that is running — or still owed — means the wallet isn't fully
    // scanned yet: keep the last step Current and don't report everything done.
    val walletScanning = phase == SyncPhase.WALLET_SCAN
    val headers = if (headersDone) StepState.Done else StepState.Current
    val blocks = blocksStepState(headersDone, blocksDone, walletScanning, hasFiltersStep)
    val filters = filtersStepState(hasFiltersStep, walletScanning, filtersDownloaded, blocksDone)
    return SyncStepStates(headers, blocks, filters, allDone = phase == SyncPhase.SYNCED)
}

private const val COMPLETE = 1f
