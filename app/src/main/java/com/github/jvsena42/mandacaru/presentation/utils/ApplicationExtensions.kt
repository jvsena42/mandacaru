package com.github.jvsena42.mandacaru.presentation.utils

import android.content.Context
import android.content.Intent

fun Context.restartApplication() {
    val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)
    Runtime.getRuntime().exit(0)
}
