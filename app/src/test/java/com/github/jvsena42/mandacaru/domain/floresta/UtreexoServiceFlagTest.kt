package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtreexoServiceFlagTest {

    @Test
    fun `returns true when 0x1000 token is present`() {
        val services = "ServiceFlags(NETWORK|WITNESS|NETWORK_LIMITED|P2P_V2|0x1000)"
        assertTrue(services.hasUtreexoServiceFlag())
    }

    @Test
    fun `returns true when combined hex token covers bit 12`() {
        // 0x3000 = UTREEXO (bit 12) | UTREEXO_ARCHIVE (bit 13)
        val services = "ServiceFlags(NETWORK|WITNESS|P2P_V2|0x3000)"
        assertTrue(services.hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false when no utreexo bit is set`() {
        val services = "ServiceFlags(NETWORK|WITNESS|NETWORK_LIMITED|P2P_V2)"
        assertFalse(services.hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false for unrelated hex tokens like 0x4000000`() {
        val services = "ServiceFlags(NETWORK|WITNESS|NETWORK_LIMITED|P2P_V2|0x4000000)"
        assertFalse(services.hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false for empty flags`() {
        assertFalse("ServiceFlags()".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false for malformed input without ServiceFlags wrapper`() {
        assertFalse("0x1000".hasUtreexoServiceFlag())
    }

    @Test
    fun `returns false when 0x1000 appears only as a substring of an unrelated token`() {
        // 0x10000 is bit 16, which is not the UTREEXO bit.
        val services = "ServiceFlags(NETWORK|0x10000)"
        assertFalse(services.hasUtreexoServiceFlag())
    }
}
