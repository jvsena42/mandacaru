package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

import androidx.compose.runtime.Stable
import com.florestad.Network


@Stable
data class SettingsUiState(
    val descriptorText: String = "",
    val signetAddress: String = "",
    val nodeAddress: String = "",
    val errorMessage: String = "",
    val selectedNetwork: String = "",
    val isLoading: Boolean = false,
    val network: List<Network> = Network.entries,
)
