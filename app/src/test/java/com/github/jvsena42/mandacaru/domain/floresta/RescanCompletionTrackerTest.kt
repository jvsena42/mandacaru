package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RescanCompletionTrackerTest {

    private val tracker = RescanCompletionTracker()

    @Test
    fun `polls before a trigger say nothing`() {
        assertNull(tracker.onPoll(rescanInProgress = false, rescanError = null))
        assertNull(tracker.onPoll(rescanInProgress = true, rescanError = null))
    }

    @Test
    fun `a rescan is completed once the node stops reporting it without an error`() {
        tracker.onTriggered()
        assertNull(tracker.onPoll(rescanInProgress = true, rescanError = null))
        assertNull(tracker.onPoll(rescanInProgress = true, rescanError = null))
        assertEquals(RescanOutcome.Completed, tracker.onPoll(rescanInProgress = false, rescanError = null))
        assertNull(tracker.onPoll(rescanInProgress = false, rescanError = null))
    }

    @Test
    fun `a rescan that ended with an error is reported as failed once`() {
        tracker.onTriggered()
        assertNull(tracker.onPoll(rescanInProgress = true, rescanError = null))
        assertEquals(
            RescanOutcome.Failed("rescan ticket was not found"),
            tracker.onPoll(rescanInProgress = false, rescanError = "rescan ticket was not found"),
        )
        assertNull(tracker.onPoll(rescanInProgress = false, rescanError = "rescan ticket was not found"))
    }

    @Test
    fun `a short rescan that finished before the first poll still completes`() {
        tracker.onTriggered()
        assertEquals(RescanOutcome.Completed, tracker.onPoll(rescanInProgress = false, rescanError = null))
    }
}
