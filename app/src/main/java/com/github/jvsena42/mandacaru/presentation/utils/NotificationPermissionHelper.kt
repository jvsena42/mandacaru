package com.github.jvsena42.mandacaru.presentation.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 13, notifications are enabled by default
            true
        }
    }

    /**
     * Register the permission launcher in the Activity
     * Call this in onCreate before setContent
     */
    fun registerPermissionLauncher(
        activity: ComponentActivity,
        onPermissionResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                onPermissionResult(isGranted)
            }
        } else {
            null
        }
    }

    /**
     * Request notification permission
     */
    fun requestNotificationPermission(
        launcher: ActivityResultLauncher<String>?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Check if we should show rationale for notification permission
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}