package com.github.jvsena42.mandacaru.presentation.service

import com.github.jvsena42.mandacaru.domain.floresta.SyncSnapshot
import com.github.jvsena42.mandacaru.domain.floresta.phase
import com.github.jvsena42.mandacaru.presentation.ui.screens.node.computeSyncStepStates
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The foreground notification and the Node screen's stepper are two views of
 * the same state and used to derive it independently — they disagreed for
 * hours at a time (jvsena42/mandacaru#103). This walks every combination and
 * asserts they still agree on the one thing that matters: are we done?
 */
class SyncNotificationParityTest {

    @Test
    fun `notification and node screen agree on being fully synced`() {
        var checked = 0
        forEachSnapshot { snapshot ->
            val phase = snapshot.phase()
            val notification = syncNotificationContent(
                phase = phase,
                snapshot = snapshot,
                headerSyncDecimal = null,
                rescanProgressDecimal = null,
                height = 947_390,
            )
            val steps = computeSyncStepStates(
                phase = phase,
                syncDecimal = snapshot.progress,
                filterSyncDecimal = snapshot.filterSyncDecimal,
            )
            assertEquals(
                "disagreed on $snapshot",
                steps.allDone,
                notification.synced,
            )
            checked++
        }
        assertEquals(EXPECTED_COMBINATIONS, checked)
    }

    @Test
    fun `every surface stays unsynced whenever a rescan is owed or running`() {
        forEachSnapshot { snapshot ->
            if (!snapshot.rescanPending && !snapshot.rescanInProgress) return@forEachSnapshot
            val phase = snapshot.phase()
            val notification = syncNotificationContent(
                phase = phase,
                snapshot = snapshot,
                headerSyncDecimal = null,
                rescanProgressDecimal = null,
                height = 947_390,
            )
            val steps = computeSyncStepStates(
                phase = phase,
                syncDecimal = snapshot.progress,
                filterSyncDecimal = snapshot.filterSyncDecimal,
            )
            assertEquals("notification claimed synced for $snapshot", false, notification.synced)
            assertEquals("node screen claimed synced for $snapshot", false, steps.allDone)
        }
    }

    private fun forEachSnapshot(block: (SyncSnapshot) -> Unit) {
        listOf(false, true).forEach { ibd ->
            listOf(0f, 0.5f, 0.9998f, 1f).forEach { progress ->
                listOf(null, 0f, 0.6785f, 1f).forEach { filters ->
                    listOf(false, true).forEach { stalled ->
                        listOf(false, true).forEach { rescanInProgress ->
                            listOf(false, true).forEach { rescanPending ->
                                block(
                                    SyncSnapshot(
                                        ibd = ibd,
                                        progress = progress,
                                        filterSyncDecimal = filters,
                                        stalled = stalled,
                                        rescanInProgress = rescanInProgress,
                                        rescanPending = rescanPending,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val EXPECTED_COMBINATIONS = 2 * 4 * 4 * 2 * 2 * 2
    }
}
