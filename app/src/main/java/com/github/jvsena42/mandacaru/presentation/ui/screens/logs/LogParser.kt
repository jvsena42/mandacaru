package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

object LogParser {

    private val LEVEL_REGEX = Regex(
        """^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\s+(ERROR|WARN|INFO|DEBUG|TRACE)\b"""
    )

    fun parse(raw: String): List<LogLine> {
        if (raw.isEmpty()) return emptyList()
        var previousLevel = LogLevel.NONE
        return raw.lineSequence().map { line ->
            val level = LEVEL_REGEX.find(line)?.let { match ->
                LogLevel.valueOf(match.groupValues[1])
            } ?: previousLevel
            previousLevel = level
            LogLine(text = line, level = level)
        }.toList()
    }
}
