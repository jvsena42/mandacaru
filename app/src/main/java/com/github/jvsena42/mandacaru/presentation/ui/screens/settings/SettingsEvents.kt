package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

sealed interface SettingsEvents {
    data object OnNetworkChanged : SettingsEvents
    data object OnNetworkPolicyChanged : SettingsEvents
    data object OnBirthdayChanged : SettingsEvents
    data class OnExportLogs(val uri: android.net.Uri) : SettingsEvents
    data class OpenReleasePage(val url: String) : SettingsEvents
    data object OpenDeveloperLogs : SettingsEvents
}
