package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import androidx.compose.runtime.Stable

@Stable
data class NodeUiState(
    val numberOfPeers: String = "",
    val blockHeight: String = "",
    val blockHash: String = "",
    val network: String = "",
    val difficulty: String = "",
    val syncPercentage: Int = 0,
    val syncDecimal: Float = 0f,
    val validatedBLocks: Int = 0,
)