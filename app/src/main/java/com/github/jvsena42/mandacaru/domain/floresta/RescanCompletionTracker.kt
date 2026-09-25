package com.github.jvsena42.mandacaru.domain.floresta

sealed interface RescanOutcome {
    data object Completed : RescanOutcome
    data class Failed(val reason: String) : RescanOutcome
}

/**
 * Follows a rescan the service triggered until the node reports it over, so the
 * `WALLET_NEEDS_RESCAN` flag is only cleared once the wallet was really scanned.
 * The node keeps the rescan in memory: an app restart, or a rescan that gave up,
 * leaves `rescan_in_progress` false just like a finished one. Clearing the flag
 * on the RPC being accepted, as before, turned those into "fully synced" with the
 * wallet half scanned (jvsena42/mandacaru#164).
 */
class RescanCompletionTracker {
    private var awaiting = false

    /** The `rescanblockchain` RPC was accepted; the node starts reporting it at once. */
    fun onTriggered() {
        awaiting = true
    }

    /**
     * A `getblockchaininfo` poll. Returns how the awaited rescan ended, or null while
     * it runs or when none was triggered by this process.
     */
    fun onPoll(rescanInProgress: Boolean, rescanError: String?): RescanOutcome? {
        if (!awaiting || rescanInProgress) return null
        awaiting = false
        return if (rescanError == null) RescanOutcome.Completed else RescanOutcome.Failed(rescanError)
    }
}
