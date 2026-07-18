package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import com.florestad.Network

sealed interface SettingsAction {
    data class OnDescriptorChanged(val descriptor: String): SettingsAction
    data class OnNodeAddressChanged(val address: String): SettingsAction
    data class OnNetworkSelected(val network: String): SettingsAction
    object OnClickUpdateDescriptor: SettingsAction
    object OnClickScanDescriptor: SettingsAction
    object OnDismissDescriptorScanner: SettingsAction
    data class OnDescriptorQrFrameScanned(val raw: String): SettingsAction
    object OnConfirmScannedDescriptor: SettingsAction
    object OnDismissScannedDescriptor: SettingsAction
    data class OnClickShareDescriptor(val descriptor: String): SettingsAction
    object OnDismissDescriptorShareSheet: SettingsAction
    object OnClickConnectNode: SettingsAction
    object OnClickRescan: SettingsAction
    object ClearSnackBarMessage: SettingsAction
    object ToggleDescriptorsExpanded: SettingsAction
    object ToggleNetworkExpanded: SettingsAction
    object ToggleNodeExpanded: SettingsAction
    object ToggleAboutExpanded: SettingsAction
    object ToggleDonateExpanded: SettingsAction
    object OnClickExportLogs: SettingsAction
    object OnClickGetUpdate: SettingsAction
    object OnClickCheckForUpdates: SettingsAction
    object ToggleBirthdayExpanded: SettingsAction
    object OnClickChangeBirthdayYear: SettingsAction
    data class OnBirthdayYearSelected(val year: Int): SettingsAction
    object OnDismissBirthdayPicker: SettingsAction
    object OnConfirmBirthdayRestart: SettingsAction
    object OnCancelBirthdayRestart: SettingsAction
    object ToggleDataUsageExpanded: SettingsAction
    data class OnToggleMobileData(val enabled: Boolean): SettingsAction
    object TogglePeerFlagsExpanded: SettingsAction
    data class OnTogglePeerFlags(val enabled: Boolean): SettingsAction
    data class OnToggleAdvancedFeatures(val enabled: Boolean): SettingsAction
    object ToggleDeveloperToolsExpanded: SettingsAction
    object OnClickViewLogs: SettingsAction
    object OnClickClearCache: SettingsAction
    object OnConfirmClearCache: SettingsAction
    object OnDismissClearCache: SettingsAction
}
