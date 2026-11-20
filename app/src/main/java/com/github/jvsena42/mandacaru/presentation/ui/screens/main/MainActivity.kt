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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.presentation.service.FlorestaService
import com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain.ScreenBlockchain
import com.github.jvsena42.mandacaru.presentation.ui.screens.logs.ScreenDeveloperLogs
import com.github.jvsena42.mandacaru.presentation.ui.screens.node.ScreenNode
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.ScreenSettings
import com.github.jvsena42.mandacaru.presentation.ui.screens.splash.SplashScreen
import com.github.jvsena42.mandacaru.presentation.ui.screens.transaction.ScreenTransaction
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.rememberAdaptiveLayout
import com.github.jvsena42.mandacaru.presentation.utils.rememberNotificationPermissionState
import com.github.jvsena42.mandacaru.presentation.utils.restartApplication
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.compose.koinViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor.TorSettingsEvent
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor.TorSettingsViewModel

class MainActivity : ComponentActivity() {

    private var serviceStartRequested = false
    private val daemon: FlorestaDaemon by inject()

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

        // Deliberately not gated on POST_NOTIFICATIONS: the service is what syncs the
        // chain, and a dataSync foreground service starts fine without it — only its
        // notification is withheld. Gating here left the node dead for the whole session
        // whenever the user declined.
        startServiceIfNeeded()

        val isColdStart = savedInstanceState == null
        enableEdgeToEdge()
        setContent {
            MandacaruTheme {
                KoinAndroidContext {
                    MandacaruRoot(
                        showSplashOnStart = isColdStart,
                        restartApplication = { stopNodeAndRestart() },
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

    /**
     * Restarting kills the process, so the node is stopped first: killed mid-write, its chain
     * database comes back corrupted and the next start has to reindex it.
     *
     * The stop runs outside this activity's scope, so a rotation or theme change while it is in
     * progress can't cancel the restart, and the request is remembered for the whole process.
     */
    private fun stopNodeAndRestart() {
        if (!restartRequested.compareAndSet(false, true)) return
        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { daemon.stopForExit() }
                .onFailure { e -> Log.e(TAG, "Error stopping the node before restart", e) }
            appContext.restartApplication()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private val restartRequested = AtomicBoolean(false)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MandacaruRoot(
    showSplashOnStart: Boolean,
    restartApplication: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel(),
) {
    var showSplash by remember { mutableStateOf(showSplashOnStart) }
    val isSettingsBadgeVisible by mainViewModel.isSettingsBadgeVisible.collectAsState()
    val needsDescriptor by mainViewModel.needsDescriptor.collectAsState()
    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(SPLASH_DURATION_MS)
            showSplash = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
    ) {
        NavDisplay(
            backStack = mainViewModel.backStack,
            onBack = { if (mainViewModel.backStack.size > 1) mainViewModel.navigateBack() },
            entryProvider = { route ->
                when (route) {
                    AppRoute.Home -> NavEntry(route) {
                        MainScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            restartApplication = restartApplication,
                            isSettingsBadgeVisible = isSettingsBadgeVisible,
                            needsDescriptor = needsDescriptor,
                            onOpenLogs = { mainViewModel.navigateTo(AppRoute.DeveloperLogs) },
                        )
                    }

                    AppRoute.DeveloperLogs -> NavEntry(route) {
                        ScreenDeveloperLogs(
                            onBack = { mainViewModel.navigateBack() },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        )
                    }
                }
            },
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
private fun EnableNotificationsSnackbar(
    needsSettings: Boolean,
    wasDenied: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    Snackbar(
        modifier = Modifier.padding(horizontal = 12.dp),
        action = {
            TextButton(onClick = onEnable) {
                Text(
                    text = stringResource(
                        if (needsSettings) {
                            R.string.notification_permission_settings
                        } else {
                            R.string.notification_permission_enable
                        }
                    ),
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.dismiss),
                )
            }
        },
    ) {
        // The tag rides the message rather than the Snackbar root: the root emits no
        // semantics of its own, so a tag there never surfaces in `android layout`.
        Text(
            text = stringResource(
                if (wasDenied) {
                    R.string.notification_permission_denied_warning
                } else {
                    R.string.notification_permission_message
                }
            ),
            modifier = Modifier.testTag("snackbar_enable_notifications"),
        )
    }
}

@Composable
private fun MainScreen(
    restartApplication: () -> Unit,
    modifier: Modifier = Modifier,
    isSettingsBadgeVisible: Boolean = false,
    needsDescriptor: Boolean = false,
    onOpenLogs: () -> Unit = {},
    torViewModel: TorSettingsViewModel = koinViewModel(),
) {
    val currentRestartApplication by rememberUpdatedState(restartApplication)
    // Collected here, not on the Settings page: leaving that page before the event arrives
    // would keep the node running without the Tor setting the user just saved.
    LaunchedEffect(torViewModel.eventFlow) {
        torViewModel.eventFlow.collect { event ->
            when (event) {
                TorSettingsEvent.RestartRequired -> currentRestartApplication()
            }
        }
    }
    val pages = Destinations.entries
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(Destinations.NODE),
        pageCount = { pages.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Rendered as plain state rather than through showSnackbar: the host's suspending call
    // can only be retracted by cancelling its coroutine, so a permission granted while it
    // was displayed left the prompt stranded on screen.
    val notificationPermission = rememberNotificationPermissionState()
    var notificationPromptDismissed by rememberSaveable { mutableStateOf(false) }
    val showNotificationPrompt = !notificationPermission.isGranted && !notificationPromptDismissed
    // The root snackbars sit above the bottom bar, over the pages' last items.
    var snackbarAreaHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val useRail = rememberAdaptiveLayout().useRail
    val onSelectDestination: (Int) -> Unit = { index ->
        coroutineScope.launch {
            pagerState.animateScrollToPage(index)
        }
    }

    var focusDescriptors by remember { mutableStateOf(false) }
    val settingsIndex = pages.indexOf(Destinations.SETTINGS)
    val onAddDescriptor: () -> Unit = remember(settingsIndex) {
        {
            focusDescriptors = true
            onSelectDestination(settingsIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.onSizeChanged {
                    snackbarAreaHeight = with(density) { it.height.toDp() }
                },
            ) {
                AnimatedVisibility(
                    visible = showNotificationPrompt,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    EnableNotificationsSnackbar(
                        needsSettings = notificationPermission.needsSettings,
                        wasDenied = notificationPermission.wasDenied,
                        onEnable = { notificationPermission.request() },
                        onDismiss = { notificationPromptDismissed = true },
                    )
                }
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        bottomBar = {
            if (!useRail) {
                AppNavigationBar(
                    pages = pages,
                    selectedIndex = pagerState.currentPage,
                    onSelect = onSelectDestination,
                    isSettingsBadgeVisible = isSettingsBadgeVisible,
                )
            }
        }
    ) { innerPadding ->
        val bottomBarPadding = innerPadding.calculateBottomPadding() + snackbarAreaHeight
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
                    isSettingsBadgeVisible = isSettingsBadgeVisible,
                )
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1
            ) { page ->
                when (pages[page]) {
                    // Both prompts anchor to the bottom of their own Scaffold, so they
                    // would sit on top of each other; the descriptor one waits its turn.
                    Destinations.NODE -> ScreenNode(
                        restartApplication = restartApplication,
                        bottomContentPadding = bottomBarPadding,
                        needsDescriptor = needsDescriptor && !showNotificationPrompt,
                        onAddDescriptor = onAddDescriptor,
                    )
                    Destinations.BLOCKCHAIN -> ScreenBlockchain(
                        bottomContentPadding = bottomBarPadding,
                    )
                    Destinations.TRANSACTION -> ScreenTransaction(
                        bottomContentPadding = bottomBarPadding,
                        needsDescriptor = needsDescriptor,
                        onAddDescriptor = onAddDescriptor,
                    )
                    Destinations.SETTINGS -> ScreenSettings(
                        restartApplication = restartApplication,
                        bottomContentPadding = bottomBarPadding,
                        onOpenLogs = onOpenLogs,
                        focusDescriptors = focusDescriptors,
                        onFocusHandled = { focusDescriptors = false },
                        torViewModel = torViewModel,
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
    isSettingsBadgeVisible: Boolean,
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
                modifier = Modifier.testTag("nav_${destination.route.lowercase()}"),
                selected = selected,
                onClick = { onSelect(index) },
                label = { DestinationLabel(destination, selected) },
                icon = { DestinationIcon(destination, showBadge(destination, isSettingsBadgeVisible)) },
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
    isSettingsBadgeVisible: Boolean,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        pages.forEachIndexed { index, destination ->
            val selected = selectedIndex == index
            NavigationRailItem(
                modifier = Modifier.testTag("nav_${destination.route.lowercase()}"),
                selected = selected,
                onClick = { onSelect(index) },
                label = { DestinationLabel(destination, selected) },
                icon = { DestinationIcon(destination, showBadge(destination, isSettingsBadgeVisible)) },
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
private fun DestinationIcon(destination: Destinations, showBadge: Boolean = false) {
    if (showBadge) {
        BadgedBox(badge = { Badge() }) {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = destination.label
            )
        }
    } else {
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = destination.label
        )
    }
}

private fun showBadge(destination: Destinations, isSettingsBadgeVisible: Boolean): Boolean =
    destination == Destinations.SETTINGS && isSettingsBadgeVisible

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
