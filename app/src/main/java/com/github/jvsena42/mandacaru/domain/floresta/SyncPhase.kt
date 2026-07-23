package com.github.jvsena42.mandacaru.domain.floresta

/**
 * The single source of truth for "how synced are we", shared by the Node
 * screen, its tablet layout and the foreground-service notification. Those
 * three used to derive it independently and drifted: the notification claimed
 * "Fully synced" for the entire compact-filter sync and for the whole wallet
 * rescan (jvsena42/mandacaru#103).
 */
enum class SyncPhase {
    HEADERS,
    STALLED,
    BLOCKS,
    FILTERS,
    WALLET_SCAN,
    SYNCED,
}

/**
 * @param filterSyncDecimal null when compact filters are disabled or started
 *   from genesis — then there is nothing to wait on.
 * @param rescanPending a rescan is owed but has not started yet
 *   (`PreferenceKeys.WALLET_NEEDS_RESCAN`). The service only fires it once the
 *   chain has been at the tip for a grace window, so without this the UI
 *   reported 100% while the wallet still had history to find.
 */
data class SyncSnapshot(
    val ibd: Boolean,
    val progress: Float,
    val filterSyncDecimal: Float?,
    val stalled: Boolean,
    val rescanInProgress: Boolean,
    val rescanPending: Boolean,
)

private const val COMPLETE = 1f

fun SyncSnapshot.phase(): SyncPhase = when {
    ibd && progress == 0f -> SyncPhase.HEADERS
    stalled -> SyncPhase.STALLED
    progress < COMPLETE -> SyncPhase.BLOCKS
    filterSyncDecimal != null && filterSyncDecimal < COMPLETE -> SyncPhase.FILTERS
    ibd -> SyncPhase.BLOCKS
    rescanInProgress || rescanPending -> SyncPhase.WALLET_SCAN
    else -> SyncPhase.SYNCED
}

/**
 * Progress of the compact-filter store towards the chain tip. A wallet
 * birthday shifts the store's start height, so the ratio is measured from
 * [filtersStart] rather than from genesis.
 */
fun computeFilterSyncDecimal(filters: Int?, filtersStart: Int?, height: Int): Float? {
    if (filters == null) return null
    val start = filtersStart ?: 0
    val numerator = (filters - start).coerceAtLeast(0).toFloat()
    val denominator = (height - start).coerceAtLeast(1).toFloat()
    return (numerator / denominator).coerceIn(0f, COMPLETE)
}

/**
 * Null while the daemon reports no block total — the caller shows an
 * indeterminate bar rather than a misleading 100%.
 */
fun computeRescanProgressDecimal(processed: Int?, total: Int?): Float? {
    val blocksTotal = total ?: 0
    if (blocksTotal <= 0) return null
    val blocksProcessed = (processed ?: 0).toFloat()
    return (blocksProcessed / blocksTotal.toFloat()).coerceIn(0f, COMPLETE)
}
