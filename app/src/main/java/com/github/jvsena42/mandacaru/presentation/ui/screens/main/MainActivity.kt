package com.github.jvsena42.mandacaru.presentation.ui.screens.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.github.jvsena42.mandacaru.presentation.service.FlorestaService
import com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain.ScreenBlockchain
import com.github.jvsena42.mandacaru.presentation.ui.screens.node.ScreenNode
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.ScreenSettings
import com.github.jvsena42.mandacaru.presentation.ui.screens.splash.SplashScreen
import com.github.jvsena42.mandacaru.presentation.ui.screens.transaction.ScreenTransaction
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.NotificationPermissionHelper
import com.github.jvsena42.mandacaru.presentation.utils.restartApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Register exit broadcast receiver
        val filter = IntentFilter(FlorestaService.ACTION_EXIT_APP)
        ContextCompat.registerReceiver(
            this,
            exitReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

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

        val isColdStart = savedInstanceState == null
        enableEdgeToEdge()
        setContent {
            MandacaruTheme {
                KoinAndroidContext {
                    MandacaruRoot(
                        showSplashOnStart = isColdStart,
                        restartApplication = { restartApplication() },
                        requestNotificationPermission = {
                            NotificationPermissionHelper.requestNotificationPermission(
                                notificationPermissionLauncher
                            )
                        },
                        hasNotificationPermission = NotificationPermissionHelper
                            .hasNotificationPermission(this@MainActivity),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(exitReceiver)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    private fun startServiceIfNeeded() {
        if (!serviceStartRequested) {
            serviceStartRequested = true
            try {
                Log.d(TAG, "Starting FlorestaService")
                startForegroundService(Intent(this, FlorestaService::class.java))
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(TAG, "Error starting service", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
private fun MandacaruRoot(
    showSplashOnStart: Boolean,
    restartApplication: () -> Unit,
    requestNotificationPermission: () -> Unit,
    hasNotificationPermission: Boolean,
) {
    var showSplash by remember { mutableStateOf(showSplashOnStart) }
    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(SPLASH_DURATION_MS)
            showSplash = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MainScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            restartApplication = restartApplication,
            requestNotificationPermission = requestNotificationPermission,
            hasNotificationPermission = hasNotificationPermission,
        )
        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = SPLASH_FADE_OUT_MS)),
        ) {
            SplashScreen()
        }
    }
}

private const val SPLASH_DURATION_MS = 4000L
private const val SPLASH_FADE_OUT_MS = 350

@Composable
private fun MainScreen(
    restartApplication: () -> Unit,
    modifier: Modifier = Modifier,
    requestNotificationPermission: () -> Unit = {},
    hasNotificationPermission: Boolean = true
) {
    val pages = Destinations.entries
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(Destinations.NODE),
        pageCount = { pages.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionSnackbar by remember { mutableStateOf(!hasNotificationPermission) }
    val currentRequestNotificationPermission by rememberUpdatedState(requestNotificationPermission)

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
                    currentRequestNotificationPermission()
                }
                showPermissionSnackbar = false
            }
        }
    }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useRail = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val onSelectDestination: (Int) -> Unit = { index ->
        coroutineScope.launch {
            pagerState.animateScrollToPage(index)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!useRail) {
                AppNavigationBar(
                    pages = pages,
                    selectedIndex = pagerState.currentPage,
                    onSelect = onSelectDestination,
                )
            }
        }
    ) { innerPadding ->
        val bottomBarPadding = innerPadding.calculateBottomPadding()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (useRail) {
                AppNavigationRail(
                    pages = pages,
                    selectedIndex = pagerState.currentPage,
                    onSelect = onSelectDestination,
                )
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1
            ) { page ->
                when (pages[page]) {
                    Destinations.NODE -> ScreenNode(
                        restartApplication = restartApplication,
                        bottomContentPadding = bottomBarPadding,
                    )
                    Destinations.BLOCKCHAIN -> ScreenBlockchain(
                        bottomContentPadding = bottomBarPadding,
                    )
                    Destinations.TRANSACTION -> ScreenTransaction(
                        bottomContentPadding = bottomBarPadding,
                    )
                    Destinations.SETTINGS -> ScreenSettings(
                        restartApplication = restartApplication,
                        bottomContentPadding = bottomBarPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    pages: List<Destinations>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
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
        pages.forEachIndexed { index, destination ->
            val selected = selectedIndex == index
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(index) },
                label = { DestinationLabel(destination, selected) },
                icon = { DestinationIcon(destination) },
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

@Composable
private fun AppNavigationRail(
    pages: List<Destinations>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        pages.forEachIndexed { index, destination ->
            val selected = selectedIndex == index
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(index) },
                label = { DestinationLabel(destination, selected) },
                icon = { DestinationIcon(destination) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DestinationLabel(destination: Destinations, selected: Boolean) {
    Text(
        destination.label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun DestinationIcon(destination: Destinations) {
    Icon(
        painter = painterResource(destination.icon),
        contentDescription = destination.label
    )
}

@PreviewLightDark
@Composable
private fun Preview() {
    MandacaruTheme {
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

@Preview(name = "Tablet", widthDp = 840, heightDp = 1280)
@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 840)
@Composable
private fun TabletPreview() {
    MandacaruTheme {
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
