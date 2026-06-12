package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import androidx.compose.runtime.Stable

@Stable
data class DeveloperLogsUiState(
    val lines: List<LogLine> = emptyList(),
    val isLoading: Boolean = true,
    val snackBarMessage: String = "",
)
