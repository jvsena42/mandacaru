package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

object LogTailReader {

    const val DEFAULT_MAX_LINES = 1000

    fun tail(lines: List<String>, max: Int = DEFAULT_MAX_LINES): List<String> {
        return if (lines.size <= max) lines else lines.takeLast(max)
    }
}
