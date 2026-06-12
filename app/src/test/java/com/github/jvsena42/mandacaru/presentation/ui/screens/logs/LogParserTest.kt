package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import org.junit.Assert.assertEquals
import org.junit.Test

class LogParserTest {

    @Test
    fun `info line maps to INFO`() {
        val result = LogParser.parse("2024-01-15 10:23:45  INFO floresta_wire::p2p_wire::peer: Handshake done")
        assertEquals(1, result.size)
        assertEquals(LogLevel.INFO, result.first().level)
    }

    @Test
    fun `warn error debug trace lines map to their levels`() {
        val raw = listOf(
            "2024-01-15 10:23:45  WARN floresta_chain: low filters",
            "2024-01-15 10:23:46 ERROR floresta_wire::transport: broken pipe",
            "2024-01-15 10:23:47 DEBUG floresta_node: tick",
            "2024-01-15 10:23:48 TRACE floresta_common: detail",
        ).joinToString("\n")

        val levels = LogParser.parse(raw).map { it.level }

        assertEquals(
            listOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.DEBUG, LogLevel.TRACE),
            levels,
        )
    }

    @Test
    fun `level token padded with extra spaces is still parsed`() {
        val result = LogParser.parse("2024-01-15 10:23:45     INFO target: msg")
        assertEquals(LogLevel.INFO, result.first().level)
    }

    @Test
    fun `continuation lines inherit the previous level`() {
        val raw = listOf(
            "2024-01-15 10:23:45 ERROR floresta_wire::transport: panic backtrace:",
            "    at frame 0",
            "    at frame 1",
        ).joinToString("\n")

        val result = LogParser.parse(raw)

        assertEquals(LogLevel.ERROR, result[0].level)
        assertEquals(LogLevel.ERROR, result[1].level)
        assertEquals(LogLevel.ERROR, result[2].level)
    }

    @Test
    fun `leading text before any log line is NONE`() {
        val result = LogParser.parse("some preamble without a timestamp")
        assertEquals(LogLevel.NONE, result.first().level)
    }

    @Test
    fun `full line text is preserved`() {
        val line = "2024-01-15 10:23:45  INFO floresta_wire::p2p_wire::peer: Handshake completed peer=3"
        val result = LogParser.parse(line)
        assertEquals(line, result.first().text)
    }

    @Test
    fun `empty input yields empty list`() {
        assertEquals(emptyList<LogLine>(), LogParser.parse(""))
    }

    @Test
    fun `a word matching a level inside the message does not change the level`() {
        val result = LogParser.parse("2024-01-15 10:23:45  INFO node: retry after ERROR response")
        assertEquals(LogLevel.INFO, result.first().level)
    }
}
