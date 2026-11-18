package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.floresta.SyncPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncRemainingTest {

    @Test
    fun `block sync counts the blocks left to validate`() {
        val remaining = syncRemaining(
            phase = SyncPhase.BLOCKS,
            headerHeight = 976_449,
            validatedBlocks = 971_324,
            filterHeight = 888_999,
        )

        assertEquals(SyncRemaining(R.plurals.sync_blocks_to_go, 5_125), remaining)
    }

    @Test
    fun `filter sync counts the filters left`() {
        val remaining = syncRemaining(
            phase = SyncPhase.FILTERS,
            headerHeight = 976_449,
            validatedBlocks = 976_449,
            filterHeight = 976_000,
        )

        assertEquals(SyncRemaining(R.plurals.sync_filters_to_go, 449), remaining)
    }

    @Test
    fun `nothing is shown when the phase is caught up or has no count`() {
        assertNull(syncRemaining(SyncPhase.BLOCKS, 100, 100, 0))
        assertNull(syncRemaining(SyncPhase.HEADERS, 100, 0, 0))
        assertNull(syncRemaining(SyncPhase.WALLET_SCAN, 100, 100, 100))
        assertNull(syncRemaining(SyncPhase.SYNCED, 100, 100, 100))
    }
}
