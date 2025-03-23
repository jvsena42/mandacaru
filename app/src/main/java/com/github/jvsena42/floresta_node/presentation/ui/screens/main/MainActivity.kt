package com.github.jvsena42.floresta_node.presentation.ui.screens.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.jvsena42.floresta_node.presentation.ui.screens.node.ScreenNode
import com.github.jvsena42.floresta_node.presentation.ui.screens.search.ScreenSearch
import com.github.jvsena42.floresta_node.presentation.ui.screens.settings.ScreenSettings
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import com.github.jvsena42.floresta_node.presentation.utils.restartApplication
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var navigationSelectedItem by remember { mutableStateOf(Destinations.NODE) }
    val navController = rememberNavController()

    Scaffold(modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.contentColorFor(
                    MaterialTheme.colorScheme.onTertiaryContainer
                ),
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
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = destination.label,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    )
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
        MainScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            restartApplication = {}
        )
    }
}