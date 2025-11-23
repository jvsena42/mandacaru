package com.github.jvsena42.floresta_node.presentation.utils

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun RequestNotificationPermissions(
    showPermissionDialog: Boolean = true,
    onPermissionChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check if permission is required (Android 13+)
    val requiresPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var isGranted by remember {
        mutableStateOf(NotificationUtils.areNotificationsEnabled(context))
    }

    // Permission request launcher
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        onPermissionChange(granted)
    }

    // Monitor lifecycle to check permission when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentPermissionState = NotificationUtils.areNotificationsEnabled(context)
                if (currentPermissionState != isGranted) {
                    isGranted = currentPermissionState
                    onPermissionChange(currentPermissionState)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request permission on first composition if needed
    DisposableEffect(Unit) {
        val currentPermissionState = NotificationUtils.areNotificationsEnabled(context)
        isGranted = currentPermissionState
        onPermissionChange(currentPermissionState)

        if (!currentPermissionState && requiresPermission && showPermissionDialog) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        onDispose { }
    }
}
