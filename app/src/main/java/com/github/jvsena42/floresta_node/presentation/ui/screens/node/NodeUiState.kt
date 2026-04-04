package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import androidx.compose.runtime.Stable
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.PeerInfoResult

@Stable
data class NodeUiState(
    val numberOfPeers: String = "",
    val blockHeight: String = "",
    val blockHash: String = "",
    val network: String = "",
    val difficulty: String = "",
    val syncPercentage: String = "0.00",
    val syncDecimal: Float = 0f,
    val validatedBLocks: Int = 0,
    val peers: List<PeerInfoResult> = emptyList(),
    val isPeersExpanded: Boolean = false,
)