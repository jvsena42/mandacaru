package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

import com.florestad.Network

sealed interface SettingsAction {
    data class OnDescriptorChanged(val descriptor: String): SettingsAction
    data class OnNodeAddressChanged(val address: String): SettingsAction
    data class OnNetworkSelected(val network: String): SettingsAction
    object OnClickUpdateDescriptor: SettingsAction
    object OnClickConnectNode: SettingsAction
    object OnClickRescan: SettingsAction
    object ClearSnackBarMessage: SettingsAction
}