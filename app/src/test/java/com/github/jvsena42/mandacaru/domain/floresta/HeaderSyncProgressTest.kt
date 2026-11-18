package com.github.jvsena42.mandacaru.domain.floresta

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeaderSyncProgressTest {

    private fun peer(initialHeight: Int) = PeerInfoResult(
        address = "1.2.3.4:8333",
        initialHeight = initialHeight,
        kind = "regular",
        services = "0000000000001009",
        state = "Ready",
        userAgent = "/Satoshi:30.0.0/",
    )

    @Test
    fun `one peer announcing a far higher height doesn't move the tip`() {
        val progress = computeHeaderSyncProgress(
            ourHeight = 976_471,
            peers = listOf(peer(976_471), peer(976_470), peer(982_541)),
        )

        assertEquals(1f, progress)
    }

    @Test
    fun `a single peer is used as the tip`() {
        assertEquals(0.5f, computeHeaderSyncProgress(ourHeight = 50, peers = listOf(peer(100))))
    }

    @Test
    fun `no progress without a known height`() {
        assertNull(computeHeaderSyncProgress(ourHeight = 50, peers = emptyList()))
        assertNull(computeHeaderSyncProgress(ourHeight = 50, peers = listOf(peer(0), peer(-1))))
        assertNull(computeHeaderSyncProgress(ourHeight = 0, peers = listOf(peer(100), peer(100))))
    }
}
