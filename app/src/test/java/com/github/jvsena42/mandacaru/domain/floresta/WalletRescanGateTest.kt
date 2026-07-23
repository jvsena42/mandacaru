package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * Virtual time throughout: the full give-up schedule spans about three hours
 * of node uptime and is asserted here without waiting for any of it.
 */
class WalletRescanGateTest {

    private val timeSource = TestTimeSource()
    private val gate = WalletRescanGate(timeSource = timeSource)

    private fun readyToTrigger() = gate.shouldTrigger(
        chainFullySynced = true,
        filtersComplete = true,
        rescanInProgress = false,
    )

    /**
     * The first call is what starts the grace clock — the service polls every
     * 10s, so in practice that is the first poll after the chain reaches the
     * tip. Leaves the gate ready to fire.
     */
    private fun armAndReachGrace() {
        assertFalse(readyToTrigger())
        timeSource += 60.seconds
        assertTrue(readyToTrigger())
    }

    @Test
    fun `does not fire before the grace window has elapsed`() {
        assertFalse(readyToTrigger())
        timeSource += 59.seconds
        assertFalse(readyToTrigger())
    }

    @Test
    fun `fires once the grace window has elapsed`() {
        assertFalse(readyToTrigger())
        timeSource += 60.seconds
        assertTrue(readyToTrigger())
    }

    @Test
    fun `does not fire while a rescan is already running`() {
        assertFalse(readyToTrigger())
        timeSource += 5.minutes
        assertFalse(
            gate.shouldTrigger(
                chainFullySynced = true,
                filtersComplete = true,
                rescanInProgress = true,
            )
        )
    }

    @Test
    fun `filters falling behind restarts the grace window`() {
        assertFalse(readyToTrigger())
        timeSource += 59.seconds
        assertFalse(
            gate.shouldTrigger(
                chainFullySynced = true,
                filtersComplete = false,
                rescanInProgress = false,
            )
        )
        // Filters back at the tip: the clock starts over, so the second that
        // would have completed the original window is not enough.
        timeSource += 1.seconds
        assertFalse(readyToTrigger())
        timeSource += 60.seconds
        assertTrue(readyToTrigger())
    }

    @Test
    fun `a failed attempt backs off twice the grace window`() {
        armAndReachGrace()
        assertFalse(gate.onFailed())

        timeSource += 119.seconds
        assertFalse(readyToTrigger())
        timeSource += 1.seconds
        assertTrue(readyToTrigger())
    }

    @Test
    fun `backoff doubles per failure and caps at thirty minutes`() {
        armAndReachGrace()

        val expectedGaps = listOf(
            2.minutes, 4.minutes, 8.minutes, 16.minutes, 30.minutes, 30.minutes,
        )
        expectedGaps.forEach { gap ->
            assertFalse(gate.onFailed())
            timeSource += gap - 1.seconds
            assertFalse("expected no retry before $gap had passed", readyToTrigger())
            timeSource += 1.seconds
            assertTrue("expected a retry once $gap had passed", readyToTrigger())
        }
    }

    @Test
    fun `gives up after ten failed attempts`() {
        armAndReachGrace()
        repeat(9) { attempt ->
            assertFalse("gave up early on attempt ${attempt + 1}", gate.onFailed())
            timeSource += 30.minutes
        }
        assertTrue(gate.onFailed())
    }

    @Test
    fun `a successful trigger resets the attempt budget`() {
        armAndReachGrace()
        repeat(9) {
            gate.onFailed()
            timeSource += 30.minutes
        }
        gate.onTriggered()

        repeat(9) {
            assertFalse(gate.onFailed())
            timeSource += 30.minutes
        }
        assertTrue(gate.onFailed())
    }

    @Test
    fun `a successful trigger also resets the backoff to the grace window`() {
        armAndReachGrace()
        gate.onFailed()
        gate.onTriggered()

        timeSource += 59.seconds
        assertFalse(readyToTrigger())
        timeSource += 1.seconds
        assertTrue(readyToTrigger())
    }
}
