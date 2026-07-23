package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The phase table behind jvsena42/mandacaru#103: the app reported
 * "Sync 100.00%" while a wallet rescan was still owed, the user reconnected
 * their Electrum wallets on the strength of it, and got partial history.
 */
class SyncPhaseTest {

    private fun snapshot(
        ibd: Boolean = false,
        progress: Float = 1f,
        filterSyncDecimal: Float? = 1f,
        stalled: Boolean = false,
        rescanInProgress: Boolean = false,
        rescanPending: Boolean = false,
    ) = SyncSnapshot(
        ibd = ibd,
        progress = progress,
        filterSyncDecimal = filterSyncDecimal,
        stalled = stalled,
        rescanInProgress = rescanInProgress,
        rescanPending = rescanPending,
    )

    @Test
    fun `rescan owed but not started yet is not synced - issue 103 steps 13 and 19`() {
        assertEquals(
            SyncPhase.WALLET_SCAN,
            snapshot(rescanInProgress = false, rescanPending = true).phase(),
        )
    }

    @Test
    fun `running rescan is a wallet scan`() {
        assertEquals(SyncPhase.WALLET_SCAN, snapshot(rescanInProgress = true).phase())
    }

    @Test
    fun `nothing owed and nothing running is synced`() {
        assertEquals(SyncPhase.SYNCED, snapshot().phase())
    }

    @Test
    fun `blocks at the tip with filters behind is a filter sync, not synced`() {
        assertEquals(SyncPhase.FILTERS, snapshot(filterSyncDecimal = 0.6785f).phase())
    }

    @Test
    fun `filters still behind outranks a pending rescan`() {
        assertEquals(
            SyncPhase.FILTERS,
            snapshot(filterSyncDecimal = 0.99f, rescanPending = true).phase(),
        )
    }

    @Test
    fun `disabled filters never hold back the synced state`() {
        assertEquals(SyncPhase.SYNCED, snapshot(filterSyncDecimal = null).phase())
    }

    @Test
    fun `ibd at zero progress is header sync`() {
        assertEquals(SyncPhase.HEADERS, snapshot(ibd = true, progress = 0f).phase())
    }

    @Test
    fun `header sync outranks a stall report`() {
        assertEquals(
            SyncPhase.HEADERS,
            snapshot(ibd = true, progress = 0f, stalled = true).phase(),
        )
    }

    @Test
    fun `stall outranks everything once headers are in`() {
        assertEquals(
            SyncPhase.STALLED,
            snapshot(stalled = true, rescanInProgress = true, rescanPending = true).phase(),
        )
    }

    @Test
    fun `partial block validation is a block sync`() {
        assertEquals(SyncPhase.BLOCKS, snapshot(ibd = true, progress = 0.9998f).phase())
    }

    @Test
    fun `still in ibd with filters at the tip falls back to blocks`() {
        assertEquals(SyncPhase.BLOCKS, snapshot(ibd = true).phase())
    }
}
