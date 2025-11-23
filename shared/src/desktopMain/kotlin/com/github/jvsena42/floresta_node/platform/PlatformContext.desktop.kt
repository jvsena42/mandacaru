package com.github.jvsena42.floresta_node.platform

import java.io.File

actual fun getDataDirectory(): String {
    val userHome = System.getProperty("user.home")
    val appDataDir = when {
        System.getProperty("os.name").lowercase().contains("win") -> {
            // Windows: %APPDATA%\FlorestaNode
            File(System.getenv("APPDATA") ?: userHome, "FlorestaNode")
        }
        System.getProperty("os.name").lowercase().contains("mac") -> {
            // macOS: ~/Library/Application Support/FlorestaNode
            File(userHome, "Library/Application Support/FlorestaNode")
        }
        else -> {
            // Linux: ~/.local/share/floresta-node
            File(userHome, ".local/share/floresta-node")
        }
    }

    // Create directory if it doesn't exist
    if (!appDataDir.exists()) {
        appDataDir.mkdirs()
    }

    return appDataDir.absolutePath
}

actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}
