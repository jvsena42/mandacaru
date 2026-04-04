package com.github.jvsena42.floresta_node.presentation.ui.screens.main

import androidx.annotation.DrawableRes
import com.github.jvsena42.floresta_node.R



enum class Destinations(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int
) {
    TRANSACTION(route = "Transaction", label = "Transactions", R.drawable.ic_transaction),
    NODE(route = "Node", label = "Node Info", R.drawable.ic_node),
    BLOCKCHAIN(route = "Blockchain", label = "Blockchain", R.drawable.ic_blockchain),
    SETTINGS(route = "Settings", label = "Settings", R.drawable.ic_settings),
}