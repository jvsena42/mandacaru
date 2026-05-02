package com.github.jvsena42.mandacaru.domain.floresta

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsLikelyStalledTest {

    private fun peer(initialHeight: Int) = PeerInfoResult(
        address = "1.2.3.4:8333",
        initialHeight = initialHeight,
        kind = "regular",
        services = "ServiceFlags(NETWORK|WITNESS|UTREEXO)",
        state = "Ready",
        userAgent = "/Satoshi:30.0.0/",
    )

    @Test
    fun `flags issue 63 scenario - daemon claims 100 percent at block 17904 while peers report tip near 947k`() {
        assertTrue(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 17_904,
                peers = listOf(peer(947_390), peer(947_391)),
            )
        )
    }

    @Test
    fun `not stalled when daemon is still in IBD`() {
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = true,
                ourHeight = 17_904,
                peers = listOf(peer(947_390)),
            )
        )
    }

    @Test
    fun `not stalled when progress is below 100 percent`() {
        assertFalse(
            isLikelyStalled(
                progress = 0.99f,
                ibd = false,
                ourHeight = 940_000,
                peers = listOf(peer(947_000)),
            )
        )
    }

    @Test
    fun `not stalled when within tolerance of peer tip`() {
        // 947_000 - 946_995 = 5 blocks behind, well within the 12-block tolerance.
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 946_995,
                peers = listOf(peer(947_000)),
            )
        )
    }

    @Test
    fun `not stalled when our height matches the peer tip`() {
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 947_000,
                peers = listOf(peer(947_000), peer(946_998)),
            )
        )
    }

    @Test
    fun `not stalled when no peers are connected to compare against`() {
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 17_904,
                peers = emptyList(),
            )
        )
    }

    @Test
    fun `uses the highest peer initial height as the tip estimate`() {
        // One outlier peer is far ahead — that's what we want to compare against.
        assertTrue(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 100_000,
                peers = listOf(peer(100_001), peer(100_005), peer(947_000)),
            )
        )
    }

    @Test
    fun `not stalled when peer reports zero or negative initial height`() {
        // Some peers may not have reported a height yet.
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 17_904,
                peers = listOf(peer(0), peer(-1)),
            )
        )
    }

    @Test
    fun `not stalled when our height is unknown - guards startup race`() {
        assertFalse(
            isLikelyStalled(
                progress = 1f,
                ibd = false,
                ourHeight = 0,
                peers = listOf(peer(947_000)),
            )
        )
    }
}
