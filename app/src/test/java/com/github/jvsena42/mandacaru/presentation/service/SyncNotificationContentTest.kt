package com.github.jvsena42.mandacaru.presentation.service

import com.github.jvsena42.mandacaru.domain.floresta.SyncPhase
import com.github.jvsena42.mandacaru.domain.floresta.SyncSnapshot
import com.github.jvsena42.mandacaru.domain.floresta.phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The foreground notification used to know only headers → blocks → stalled →
 * synced, so it read "Fully synced" for the whole filter sync (hours) and for
 * the whole wallet rescan while the Node screen said otherwise
 * (jvsena42/mandacaru#103).
 */
class SyncNotificationContentTest {

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

    private fun content(
        snapshot: SyncSnapshot,
        headerSyncDecimal: Float? = null,
        rescanProgressDecimal: Float? = null,
        height: Int = 947_390,
    ) = syncNotificationContent(
        phase = snapshot.phase(),
        snapshot = snapshot,
        headerSyncDecimal = headerSyncDecimal,
        rescanProgressDecimal = rescanProgressDecimal,
        height = height,
    )

    @Test
    fun `does not claim synced while filters are still downloading`() {
        val result = content(snapshot(filterSyncDecimal = 0.6785f))

        assertFalse(result.synced)
        assertEquals("Syncing filters: 67.85%", result.contentText)
        assertEquals("67.85% filters", result.subText)
        assertEquals(67, result.progressPercent)
    }

    @Test
    fun `does not claim synced while a rescan is owed but not started`() {
        val result = content(snapshot(rescanPending = true))

        assertFalse(result.synced)
        assertEquals("Scanning wallet…", result.contentText)
        assertEquals("Finding your transactions", result.subText)
        assertTrue(result.indeterminate)
    }

    @Test
    fun `shows real rescan progress once the daemon reports a block total`() {
        val result = content(
            snapshot(rescanInProgress = true),
            rescanProgressDecimal = 0.42f,
        )

        assertFalse(result.synced)
        assertEquals("Scanning wallet: 42.00%", result.contentText)
        assertEquals(42, result.progressPercent)
        assertFalse(result.indeterminate)
    }

    @Test
    fun `claims synced only when nothing is left to do`() {
        val result = content(snapshot())

        assertTrue(result.synced)
        assertEquals("Synced - Block #947,390", result.contentText)
        assertEquals("Fully synced", result.subText)
        assertNull(result.progressPercent)
    }

    @Test
    fun `header sync shows peer-derived progress when available`() {
        val result = content(
            snapshot(ibd = true, progress = 0f),
            headerSyncDecimal = 0.5f,
        )

        assertEquals("Syncing headers: 50.00%", result.contentText)
        assertEquals(50, result.progressPercent)
    }

    @Test
    fun `header sync without peers is indeterminate`() {
        val result = content(snapshot(ibd = true, progress = 0f))

        assertEquals("Syncing headers…", result.contentText)
        assertEquals("Connecting to peers", result.subText)
        assertTrue(result.indeterminate)
    }

    @Test
    fun `block sync reports the validation percentage`() {
        val result = content(snapshot(ibd = true, progress = 0.9998f))

        assertEquals("Syncing blocks: 99.98%", result.contentText)
        assertEquals("99.98% blocks", result.subText)
    }

    @Test
    fun `stalled sync names the height it is stuck at and shows no bar`() {
        val result = content(snapshot(stalled = true), height = 17_904)

        assertFalse(result.synced)
        assertEquals("Sync stalled at block #17,904", result.contentText)
        assertEquals("Storage may be unhealthy", result.subText)
        assertNull(result.progressPercent)
        assertFalse(result.indeterminate)
    }

    @Test
    fun `synced is the only phase that reports synced`() {
        SyncPhase.entries.forEach { phase ->
            val result = syncNotificationContent(
                phase = phase,
                snapshot = snapshot(),
                headerSyncDecimal = null,
                rescanProgressDecimal = null,
                height = 947_390,
            )
            assertEquals("$phase reported the wrong synced state", phase == SyncPhase.SYNCED, result.synced)
        }
    }
}
