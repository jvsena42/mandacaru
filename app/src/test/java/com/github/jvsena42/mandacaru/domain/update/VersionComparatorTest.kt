package com.github.jvsena42.mandacaru.domain.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `newer patch is detected`() {
        assertTrue(VersionComparator.isNewer("0.10.4", "0.10.3"))
    }

    @Test
    fun `newer minor is detected`() {
        assertTrue(VersionComparator.isNewer("0.11.0", "0.10.9"))
    }

    @Test
    fun `newer major is detected`() {
        assertTrue(VersionComparator.isNewer("1.0.0", "0.99.99"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(VersionComparator.isNewer("0.10.3", "0.10.3"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(VersionComparator.isNewer("0.10.2", "0.10.3"))
    }

    @Test
    fun `leading v prefix is ignored`() {
        assertTrue(VersionComparator.isNewer("v0.10.4", "0.10.3"))
        assertFalse(VersionComparator.isNewer("v0.10.3", "0.10.3"))
    }

    @Test
    fun `different segment counts compare correctly`() {
        assertTrue(VersionComparator.isNewer("0.10.3.1", "0.10.3"))
        assertFalse(VersionComparator.isNewer("0.10", "0.10.0"))
    }

    @Test
    fun `non numeric suffixes are tolerated`() {
        assertTrue(VersionComparator.isNewer("0.11.0-rc1", "0.10.3"))
    }

    @Test
    fun `uppercase V prefix is ignored`() {
        assertTrue(VersionComparator.isNewer("V0.10.4", "0.10.3"))
        assertFalse(VersionComparator.isNewer("V0.10.3", "0.10.3"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertTrue(VersionComparator.isNewer("  0.10.4  ", "0.10.3"))
        assertFalse(VersionComparator.isNewer("0.10.3", "  0.10.3  "))
    }

    @Test
    fun `a prerelease of the same version is not treated as newer`() {
        // "0.11.0-rc1" parses to 0.11.0, equal to the release — not newer.
        assertFalse(VersionComparator.isNewer("0.11.0-rc1", "0.11.0"))
    }
}
