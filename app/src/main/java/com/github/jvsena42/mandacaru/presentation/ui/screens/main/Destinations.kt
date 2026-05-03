package com.github.jvsena42.mandacaru.presentation.ui.screens.main

import androidx.annotation.DrawableRes
import com.github.jvsena42.mandacaru.R



enum class Destinations(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int
) {
    NODE(route = "Node", label = "Node Info", R.drawable.ic_node),
    BLOCKCHAIN(route = "Blockchain", label = "Blockchain", R.drawable.ic_blockchain),
    TRANSACTION(route = "Transaction", label = "Transactions", R.drawable.ic_transaction),
    SETTINGS(route = "Settings", label = "Settings", R.drawable.ic_settings),
}
