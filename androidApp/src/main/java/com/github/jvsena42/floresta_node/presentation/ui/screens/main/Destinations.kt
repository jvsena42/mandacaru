package com.github.jvsena42.floresta_node.presentation.ui.screens.main

import androidx.annotation.DrawableRes
import com.github.jvsena42.floresta_node.R



enum class Destinations(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int
) {
    SEARCH(route = "Search", label = "Search", R.drawable.ic_search),
    NODE(route = "Node", label = "Node Info", R.drawable.ic_node),
    SETTINGS(route = "Settings", label = "Settings", R.drawable.ic_settings),
}