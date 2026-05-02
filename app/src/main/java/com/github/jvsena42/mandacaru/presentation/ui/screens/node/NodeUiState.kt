package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.runtime.Stable
import com.github.jvsena42.mandacaru.domain.floresta.SnapshotPreview
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
    val headerHeightRaw: Int = 0,
    val headerSyncDecimal: Float? = null,
    val headerSyncPercentage: String = "0.00",
    val filterHeightRaw: Int = 0,
    val filterSyncDecimal: Float? = null,
    val filterSyncPercentage: String = "0.00",
    val isStalled: Boolean = false,
    val ibd: Boolean = true,
    val validatedBLocks: Int = 0,
    val peers: List<PeerInfoResult> = emptyList(),
    val utreexoPeerCount: Int = 0,
    val isPeersExpanded: Boolean = false,
    val uptime: String = "",
    val isDiagnosticsExpanded: Boolean = false,

    val isScanSheetOpen: Boolean = false,
    val isPasteSheetOpen: Boolean = false,
    val isExportQrSheetOpen: Boolean = false,
    val isImportCardExpanded: Boolean = true,
    val isExportCardExpanded: Boolean = false,
    val pendingSnapshotPreview: SnapshotPreview? = null,
    val pendingSnapshotPayload: String? = null,
    val exportPayload: String? = null,
    val snapshotMessage: String? = null,
    val isApplyingSnapshot: Boolean = false,
)
