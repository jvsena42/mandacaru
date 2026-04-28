package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import com.florestad.Network

sealed interface SettingsAction {
    data class OnDescriptorChanged(val descriptor: String): SettingsAction
    data class OnNodeAddressChanged(val address: String): SettingsAction
    data class OnNetworkSelected(val network: String): SettingsAction
    object OnClickUpdateDescriptor: SettingsAction
    object OnClickConnectNode: SettingsAction
    object OnClickRescan: SettingsAction
    object ClearSnackBarMessage: SettingsAction
    object ToggleDescriptorsExpanded: SettingsAction
    object ToggleNetworkExpanded: SettingsAction
    object ToggleNodeExpanded: SettingsAction
    object ToggleAboutExpanded: SettingsAction
    object ToggleDonateExpanded: SettingsAction
    object OnClickExportLogs: SettingsAction
    object ToggleBirthdayExpanded: SettingsAction
    object OnClickChangeBirthdayYear: SettingsAction
    data class OnBirthdayYearSelected(val year: Int): SettingsAction
    object OnDismissBirthdayPicker: SettingsAction
    object OnConfirmBirthdayRestart: SettingsAction
    object OnCancelBirthdayRestart: SettingsAction
}
