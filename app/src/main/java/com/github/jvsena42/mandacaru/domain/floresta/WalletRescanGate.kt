package com.github.jvsena42.mandacaru.domain.floresta

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Decides when the service may fire the follow-up `rescanblockchain` armed by
 * a descriptor load or a birthday change.
 *
 * Filter sync usually catches up to chain sync within seconds, but
 * matched-block downloads add tail latency, so we wait out [grace] after the
 * chain reports fully synced before rescanning. Failed attempts back off
 * geometrically and give up after [maxAttempts]: the UI now reports "not
 * synced" while a rescan is owed, so an unfixable failure (e.g. filters
 * disabled) must not pin it there forever.
 *
 * Timing runs off a monotonic [TimeSource], not the wall clock — an NTP
 * correction mid-session would otherwise make a long backoff fire immediately
 * or never. Deep sleep freezes both this and the poll loop that drives it, so
 * the two stay consistent.
 */
class WalletRescanGate(
    private val grace: Duration = 60.seconds,
    private val maxAttempts: Int = 10,
    private val maxBackoff: Duration = 30.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var fullySyncedSince: TimeMark? = null
    private var lastAttempt: TimeMark? = null
    private var failures = 0

    fun shouldTrigger(
        chainFullySynced: Boolean,
        filtersComplete: Boolean,
        rescanInProgress: Boolean,
    ): Boolean {
        if (!chainFullySynced || !filtersComplete || rescanInProgress) {
            fullySyncedSince = null
            return false
        }
        val syncedSince = fullySyncedSince ?: timeSource.markNow().also { fullySyncedSince = it }
        if (syncedSince.elapsedNow() < grace) return false
        val since = lastAttempt ?: return true
        return since.elapsedNow() >= backoff()
    }

    /** The rescan RPC was accepted; the attempt budget resets. */
    fun onTriggered() {
        lastAttempt = timeSource.markNow()
        failures = 0
    }

    /**
     * The rescan RPC failed.
     *
     * @return true once the budget is spent, meaning the caller should clear
     *   `WALLET_NEEDS_RESCAN` and stop holding the UI back.
     */
    fun onFailed(): Boolean {
        lastAttempt = timeSource.markNow()
        failures++
        return failures >= maxAttempts
    }

    private fun backoff(): Duration {
        if (failures <= 0) return grace
        val doublings = failures.coerceAtMost(MAX_DOUBLINGS)
        val scaled = grace * (1L shl doublings).toDouble()
        return minOf(scaled, maxBackoff)
    }

    private companion object {
        // Beyond this the backoff has long since hit maxBackoff; capping the
        // shift keeps it away from Long overflow.
        const val MAX_DOUBLINGS = 20
    }
}
