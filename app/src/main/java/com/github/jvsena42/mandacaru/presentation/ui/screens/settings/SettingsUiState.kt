package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import androidx.compose.runtime.Stable
import com.florestad.Network


@Stable
data class SettingsUiState(
    val descriptorText: String = "",
    val electrumAddress: String = "",
    val nodeAddress: String = "",
    val snackBarMessage: String = "",
    val selectedNetwork: String = "",
    val isLoading: Boolean = false,
    val descriptors: List<String> = emptyList(),
    val network: List<Network> = Network.entries,
    val isDescriptorsExpanded: Boolean = false,
    val isNetworkExpanded: Boolean = false,
    val isNodeExpanded: Boolean = false,
    val isAboutExpanded: Boolean = false,
    val isDonateExpanded: Boolean = false,
)
