package com.github.jvsena42.mandacaru.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryFlagTest {

    @Test
    fun `maps BR to the regional indicator pair`() {
        val flag = countryCodeToFlagEmoji("BR")
        assertEquals(listOf(0x1F1E7, 0x1F1F7), flag?.codePoints()?.toArray()?.toList())
    }

    @Test
    fun `maps US to the regional indicator pair`() {
        assertEquals(listOf(0x1F1FA, 0x1F1F8), countryCodeToFlagEmoji("US")?.codePoints()?.toArray()?.toList())
    }

    @Test
    fun `normalizes lowercase`() {
        assertEquals(countryCodeToFlagEmoji("BR"), countryCodeToFlagEmoji("br"))
    }

    @Test
    fun `flag is two surrogate pairs`() {
        assertEquals(4, countryCodeToFlagEmoji("DE")?.length)
    }

    @Test
    fun `rejects the unknown marker`() {
        assertNull(countryCodeToFlagEmoji("??"))
    }

    @Test
    fun `rejects wrong length`() {
        assertNull(countryCodeToFlagEmoji(""))
        assertNull(countryCodeToFlagEmoji("X"))
        assertNull(countryCodeToFlagEmoji("BRA"))
    }

    @Test
    fun `rejects non-letters`() {
        assertNull(countryCodeToFlagEmoji("B1"))
        assertNull(countryCodeToFlagEmoji("1B"))
        assertNull(countryCodeToFlagEmoji("--"))
    }

    @Test
    fun `unassigned but well-formed codes still map`() {
        // We render whatever the database says; we do not police ISO assignment.
        assertEquals(4, countryCodeToFlagEmoji("ZZ")?.length)
    }

    @Test
    fun `display name resolves known country`() {
        // Asserted without naming the string: the JVM default locale decides whether this is
        // "Brazil" or "Brasil", so only the contract is pinned — a human name, not the code.
        val name = countryCodeToDisplayName("BR")
        assertNotEquals("BR", name)
        assertTrue(name.length > 2)
    }

    @Test
    fun `display name falls back to the code when unassigned`() {
        assertEquals("QQ", countryCodeToDisplayName("QQ"))
    }
}
