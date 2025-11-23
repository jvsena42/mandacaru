package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

sealed interface SettingsEvents {
    data object OnNetworkChanged : SettingsEvents
}