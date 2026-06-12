package com.github.jvsena42.mandacaru.presentation.ui.screens.main

sealed interface AppRoute {
    data object Home : AppRoute
    data object DeveloperLogs : AppRoute
}
