package com.github.jvsena42.floresta_node.platform

import android.util.Log

actual fun getDataDirectory(): String {
    return androidContext.filesDir.toString()
}

actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}
