package com.github.jvsena42.mandacaru.domain.floresta

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import kotlin.math.roundToInt

private const val STALL_TOLERANCE_BLOCKS = 12
private const val MIN_PEERS_AHEAD_FOR_STALL = 2

fun computeHeaderSyncProgress(
    ourHeight: Int,
    peers: List<PeerInfoResult>,
): Float? {
    val tip = peers.maxOfOrNull { it.initialHeight } ?: return null
    if (tip <= 0 || ourHeight <= 0) return null
    val ratio = (ourHeight.toFloat() / tip.toFloat()).coerceIn(0f, 1f)
    return (ratio * 10000f).roundToInt() / 10000f
}

/**
 * Returns true when the daemon's `getblockchaininfo` claims sync is complete
 * (`progress >= 1f`, `!ibd`) but our local height is meaningfully behind what
 * peers report as their initial height — i.e. the chain is jammed early, not
 * actually at the tip.
 *
 * Floresta computes `progress` as `validated / height`, so when both numbers
 * stall together (e.g. `flat_chain_store` `IndexIsFull` on flaky storage), the
 * daemon reports 100% even though headers never reached the tip.
 *
 * At least two peers must report a height past the tolerance: a single peer can
 * announce any height, and one doing so must not tell the user to reinstall.
 */
fun isLikelyStalled(
    progress: Float,
    ibd: Boolean,
    ourHeight: Int,
    peers: List<PeerInfoResult>,
): Boolean {
    if (ibd || progress < 1f || ourHeight <= 0) return false
    val peersAhead = peers.count { it.initialHeight > ourHeight + STALL_TOLERANCE_BLOCKS }
    return peersAhead >= MIN_PEERS_AHEAD_FOR_STALL
}
