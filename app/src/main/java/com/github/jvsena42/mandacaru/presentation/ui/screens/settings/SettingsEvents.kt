package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

sealed interface SettingsEvents {
    data object OnNetworkChanged : SettingsEvents
    data class OnExportLogs(val uri: android.net.Uri) : SettingsEvents
}
