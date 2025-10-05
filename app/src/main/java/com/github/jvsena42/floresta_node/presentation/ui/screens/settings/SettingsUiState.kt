package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

import androidx.compose.runtime.Stable
import com.florestad.Network


@Stable
data class SettingsUiState(
    val descriptorText: String = "",
    val electrumAddress: String = "",
    val nodeAddress: String = "",
    val errorMessage: String = "",
    val selectedNetwork: String = "",
    val isLoading: Boolean = false,
    val descriptors: List<String> = emptyList(),
    val network: List<Network> = Network.entries,
    val isDescriptorsExpanded: Boolean = false,
    val isNetworkExpanded: Boolean = false,
    val isNodeExpanded: Boolean = false,
)