package com.github.jvsena42.floresta_node

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.github.jvsena42.floresta_node.presentation.App

fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Floresta Node",
        state = windowState
    ) {
        App()
    }
}
