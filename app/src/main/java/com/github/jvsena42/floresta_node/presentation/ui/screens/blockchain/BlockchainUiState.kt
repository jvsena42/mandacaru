package com.github.jvsena42.floresta_node.presentation.ui.screens.blockchain

import androidx.compose.runtime.Stable
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.BlockHeaderResult

@Stable
data class BlockchainUiState(
    val blockCount: String = "",
    val bestBlockHash: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val blockHeader: BlockHeaderResult? = null,
    val blockHash: String = "",
    val blockHeight: String = "",
    val validatedBlocks: Int = 0,
)
