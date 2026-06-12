package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import org.junit.Assert.assertEquals
import org.junit.Test

class LogTailReaderTest {

    @Test
    fun `fewer lines than max returns all lines`() {
        val lines = listOf("a", "b", "c")
        assertEquals(lines, LogTailReader.tail(lines, max = 10))
    }

    @Test
    fun `more lines than max returns exactly the last max`() {
        val lines = (1..100).map { it.toString() }
        val result = LogTailReader.tail(lines, max = 10)
        assertEquals((91..100).map { it.toString() }, result)
    }

    @Test
    fun `exactly max returns all lines`() {
        val lines = (1..10).map { it.toString() }
        assertEquals(lines, LogTailReader.tail(lines, max = 10))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals(emptyList<String>(), LogTailReader.tail(emptyList(), max = 10))
    }
}
