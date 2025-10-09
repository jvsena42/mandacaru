package com.github.jvsena42.floresta_node.presentation.ui.screens.main

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaService.Companion.CHANNEL_ID
import com.github.jvsena42.floresta_node.presentation.ui.screens.node.ScreenNode
import com.github.jvsena42.floresta_node.presentation.ui.screens.search.ScreenSearch
import com.github.jvsena42.floresta_node.presentation.ui.screens.settings.ScreenSettings
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import com.github.jvsena42.floresta_node.presentation.utils.RequestNotificationPermissions
import com.github.jvsena42.floresta_node.presentation.utils.initNotificationChannel
import com.github.jvsena42.floresta_node.presentation.utils.restartApplication
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            initNotificationChannel(
                id = CHANNEL_ID,
                name = "Floresta node notification",
                desc = "Channel for Floresta node service",
                importance = NotificationManager.IMPORTANCE_LOW
            )

            startForegroundService(Intent(this, FlorestaService::class.java))
        }.onFailure { exception ->
            Log.e("MainActivity", "Failure stating service", exception)
        }

        enableEdgeToEdge()
        setContent {
            FlorestaNodeTheme {
                KoinAndroidContext {
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        restartApplication = { restartApplication() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    modifier: Modifier = Modifier,
    restartApplication: () -> Unit
) {
    var navigationSelectedItem by rememberSaveable { mutableStateOf(Destinations.NODE) }
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
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
            composable(Destinations.SEARCH.route) {
                ScreenSearch()
            }
            composable(Destinations.NODE.route) {
                ScreenNode()
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