package com.github.jvsena42.mandacaru.domain.floresta

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import kotlin.math.roundToInt

fun computeHeaderSyncProgress(
    ourHeight: Int,
    peers: List<PeerInfoResult>,
): Float? {
    val tip = peers.maxOfOrNull { it.initialHeight } ?: return null
    if (tip <= 0 || ourHeight <= 0) return null
    val ratio = (ourHeight.toFloat() / tip.toFloat()).coerceIn(0f, 1f)
    return (ratio * 10000f).roundToInt() / 10000f
}
