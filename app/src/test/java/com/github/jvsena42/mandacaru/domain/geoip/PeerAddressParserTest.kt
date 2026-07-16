package com.github.jvsena42.mandacaru.domain.geoip

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.Inet4Address
import java.net.Inet6Address

class PeerAddressParserTest {

    // --- IPv4 ---

    @Test
    fun `parses IPv4 with port`() {
        val parsed = PeerAddressParser.parse("194.145.199.26:8333")
        assertEquals("194.145.199.26", parsed?.hostAddress)
    }

    @Test
    fun `parses IPv4 without port`() {
        val parsed = PeerAddressParser.parse("59.3.9.212")
        assertEquals("59.3.9.212", parsed?.hostAddress)
    }

    @Test
    fun `parses IPv4 boundary octets`() {
        assertEquals("0.0.0.0", PeerAddressParser.parse("0.0.0.0:8333")?.hostAddress)
        assertEquals("255.255.255.255", PeerAddressParser.parse("255.255.255.255")?.hostAddress)
    }

    @Test
    fun `IPv4 yields an Inet4Address`() {
        assertEquals(true, PeerAddressParser.parse("8.8.8.8:8333") is Inet4Address)
    }

    @Test
    fun `rejects out-of-range octet`() {
        assertNull(PeerAddressParser.parse("256.1.1.1:8333"))
    }

    @Test
    fun `rejects short IPv4`() {
        assertNull(PeerAddressParser.parse("1.2.3"))
    }

    // --- IPv6 ---

    @Test
    fun `parses bracketed IPv6 with port`() {
        val parsed = PeerAddressParser.parse("[2001:db8::1]:8333")
        assertEquals(true, parsed is Inet6Address)
        assertArrayEquals(
            byteArrayOf(0x20, 0x01, 0x0d, 0xb8.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            parsed?.address,
        )
    }

    @Test
    fun `parses bracketed IPv6 without port`() {
        assertEquals(true, PeerAddressParser.parse("[2001:db8::1]") is Inet6Address)
    }

    @Test
    fun `parses unbracketed IPv6`() {
        assertEquals(true, PeerAddressParser.parse("2001:4860:4860::8888") is Inet6Address)
    }

    @Test
    fun `parses fully expanded IPv6`() {
        val parsed = PeerAddressParser.parse("[2001:0db8:0000:0000:0000:0000:0000:0001]:8333")
        assertArrayEquals(
            byteArrayOf(0x20, 0x01, 0x0d, 0xb8.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            parsed?.address,
        )
    }

    @Test
    fun `parses loopback and all-zero IPv6`() {
        assertArrayEquals(ByteArray(16).also { it[15] = 1 }, PeerAddressParser.parse("[::1]:8333")?.address)
        assertArrayEquals(ByteArray(16), PeerAddressParser.parse("::")?.address)
    }

    @Test
    fun `IPv4-mapped IPv6 unwraps to the embedded IPv4`() {
        // Java collapses ::ffff:a.b.c.d to an Inet4Address, which is what we want for lookup.
        assertEquals("1.2.3.4", PeerAddressParser.parse("[::ffff:1.2.3.4]:8333")?.hostAddress)
    }

    @Test
    fun `rejects IPv6 with two compression runs`() {
        assertNull(PeerAddressParser.parse("[2001::db8::1]:8333"))
    }

    @Test
    fun `rejects IPv6 with too many groups`() {
        assertNull(PeerAddressParser.parse("[1:2:3:4:5:6:7:8:9]:8333"))
    }

    @Test
    fun `rejects uncompressed IPv6 with too few groups`() {
        assertNull(PeerAddressParser.parse("[1:2:3:4:5:6:7]:8333"))
    }

    @Test
    fun `rejects IPv6 group with non-hex digits`() {
        assertNull(PeerAddressParser.parse("[2001:db8::zz]:8333"))
    }

    @Test
    fun `rejects unclosed bracket`() {
        assertNull(PeerAddressParser.parse("[2001:db8::1:8333"))
    }

    // --- Non-IP: must never reach DNS ---

    @Test
    fun `rejects onion address`() {
        assertNull(PeerAddressParser.parse("expyuzz4wqqyqhjn.onion:8333"))
    }

    @Test
    fun `rejects hostname`() {
        assertNull(PeerAddressParser.parse("seed.bitcoin.sipa.be:8333"))
    }

    @Test
    fun `rejects localhost`() {
        assertNull(PeerAddressParser.parse("localhost:8333"))
    }

    @Test
    fun `rejects empty and blank`() {
        assertNull(PeerAddressParser.parse(""))
        assertNull(PeerAddressParser.parse("   "))
    }

    @Test
    fun `rejects garbage`() {
        assertNull(PeerAddressParser.parse("not an address"))
        assertNull(PeerAddressParser.parse(":::"))
    }
}
