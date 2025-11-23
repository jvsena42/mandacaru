package com.github.jvsena42.floresta_node.presentation.utils

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

fun Context.restartApplication() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
    exitProcess(0)
}