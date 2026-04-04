package com.github.jvsena42.mandacaru.presentation.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE

fun Context.initNotificationChannel(
    id: String = "Floresta notificaton channel",
    name: String = "App notification",
    desc: String = "channel_app",
    importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
) {
    val channel = NotificationChannel(id, name, importance).apply { description = desc }
    notificationManager.createNotificationChannel(channel)
}

val Context.notificationManager: NotificationManager
    get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
