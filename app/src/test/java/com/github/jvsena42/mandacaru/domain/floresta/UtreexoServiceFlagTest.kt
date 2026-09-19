package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtreexoServiceFlagTest {

    @Test
    fun `returns true when bit 12 is set`() {
        // NETWORK | WITNESS | NETWORK_LIMITED | P2P_V2 | UTREEXO
        assertTrue("0000000000001c09".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns true when utreexo and utreexo archive are both set`() {
        assertTrue("0000000000003c09".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false when no utreexo bit is set`() {
        // NETWORK | WITNESS | COMPACT_FILTERS | NETWORK_LIMITED | P2P_V2
        assertFalse("0000000000000c49".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false when only the archive bit is set`() {
        assertFalse("0000000000002009".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns true when the highest bit is also set`() {
        assertTrue("8000000000001009".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false for empty input`() {
        assertFalse("".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false for the legacy ServiceFlags format`() {
        assertFalse("ServiceFlags(NETWORK|WITNESS|0x1000)".hasUtreexoServiceFlag())
    }
}
