package com.github.jvsena42.mandacaru.domain.geoip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerOnionTest {

    private val onion = "vfwnj3gidfsuc4kkerx4uzgoqyynz2n5evnbeuyxmmhvelplrhqvzhid.onion"

    @Test
    fun `recognises an onion peer with and without a port`() {
        assertTrue(PeerAddressParser.isOnion("$onion:8333"))
        assertTrue(PeerAddressParser.isOnion(onion.uppercase()))
    }

    @Test
    fun `IP peers are not onion`() {
        assertFalse(PeerAddressParser.isOnion("194.145.199.26:8333"))
        assertFalse(PeerAddressParser.isOnion("[2001:db8::1]:8333"))
        assertFalse(PeerAddressParser.isOnion("onion.example.com:8333"))
    }

    @Test
    fun `onion peers never resolve to an IP for GeoIP`() {
        assertNull(PeerAddressParser.parse("$onion:8333"))
    }
}
