package com.github.jvsena42.floresta_node.presentation.ui.screens.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaService
import com.github.jvsena42.floresta_node.presentation.ui.screens.blockchain.ScreenBlockchain
import com.github.jvsena42.floresta_node.presentation.ui.screens.node.ScreenNode
import com.github.jvsena42.floresta_node.presentation.ui.screens.transaction.ScreenTransaction
import com.github.jvsena42.floresta_node.presentation.ui.screens.settings.ScreenSettings
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import com.github.jvsena42.floresta_node.presentation.utils.NotificationPermissionHelper
import com.github.jvsena42.floresta_node.presentation.utils.restartApplication
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {

    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var serviceStartRequested = false

    private val exitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FlorestaService.ACTION_EXIT_APP) {
                Log.d(TAG, "Exit broadcast received, finishing activity")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register exit broadcast receiver
        val filter = IntentFilter(FlorestaService.ACTION_EXIT_APP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(exitReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(exitReceiver, filter)
        }

        // Register permission launcher before setContent
        notificationPermissionLauncher = NotificationPermissionHelper.registerPermissionLauncher(
            activity = this,
            onPermissionResult = { isGranted ->
                Log.d(TAG, "Notification permission result: $isGranted")
                if (isGranted) {
                    // Permission granted, start service if not already started
                    startServiceIfNeeded()
                }
            }
        )

        // Request permission immediately if not granted
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            Log.d(TAG, "Requesting notification permission")
            NotificationPermissionHelper.requestNotificationPermission(
                notificationPermissionLauncher
            )
        } else {
            // Permission already granted, start service immediately
            Log.d(TAG, "Permission already granted, starting service")
            startServiceIfNeeded()
        }

        enableEdgeToEdge()
        setContent {
            FlorestaNodeTheme {
                KoinAndroidContext {
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        restartApplication = { restartApplication() },
                        requestNotificationPermission = {
                            NotificationPermissionHelper.requestNotificationPermission(
                                notificationPermissionLauncher
                            )
                        },
                        hasNotificationPermission = NotificationPermissionHelper.hasNotificationPermission(this)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(exitReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    private fun startServiceIfNeeded() {
        if (!serviceStartRequested) {
            serviceStartRequested = true
            try {
                Log.d(TAG, "Starting FlorestaService")
                startForegroundService(Intent(this, FlorestaService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
private fun MainScreen(
    modifier: Modifier = Modifier,
    restartApplication: () -> Unit,
    requestNotificationPermission: () -> Unit = {},
    hasNotificationPermission: Boolean = true
) {
    var navigationSelectedItem by rememberSaveable { mutableStateOf(Destinations.NODE) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionSnackbar by remember { mutableStateOf(!hasNotificationPermission) }

    // Show snackbar if permission was denied
    LaunchedEffect(hasNotificationPermission) {
        if (!hasNotificationPermission && showPermissionSnackbar) {
            launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Enable notifications to see when the node is running",
                    actionLabel = "Enable",
                    withDismissAction = true
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    requestNotificationPermission()
                }
                showPermissionSnackbar = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Destinations.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == navigationSelectedItem,
                            onClick = {
                                navigationSelectedItem = destination
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = {
                                Text(
                                    destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (destination == navigationSelectedItem)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.icon),
                                    contentDescription = destination.label
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.NODE.route,
            modifier = Modifier.padding(paddingValues = innerPadding)
        ) {
            composable(Destinations.TRANSACTION.route) {
                ScreenTransaction()
            }
            composable(Destinations.NODE.route) {
                ScreenNode()
            }
            composable(Destinations.BLOCKCHAIN.route) {
                ScreenBlockchain()
            }
            composable(Destinations.SETTINGS.route) {
                ScreenSettings(
                    restartApplication = restartApplication
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    FlorestaNodeTheme {
        Surface {
            MainScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                restartApplication = {}
            )
        }
    }
}