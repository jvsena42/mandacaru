package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.runtime.Stable
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult

@Stable
data class NodeUiState(
    val numberOfPeers: String = "",
    val blockHeight: String = "",
    val blockHash: String = "",
    val network: String = "",
    val difficulty: String = "",
    val syncPercentage: String = "0.00",
    val syncDecimal: Float = 0f,
    val ibd: Boolean = false,
    val validatedBLocks: Int = 0,
    val peers: List<PeerInfoResult> = emptyList(),
    val isPeersExpanded: Boolean = false,
    val uptime: String = "",
    val memoryUsed: String = "",
    val memoryFree: String = "",
    val memoryTotal: String = "",
    val isDiagnosticsExpanded: Boolean = false,
)
