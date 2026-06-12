package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

enum class LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    NONE,
}

data class LogLine(
    val text: String,
    val level: LogLevel,
)
