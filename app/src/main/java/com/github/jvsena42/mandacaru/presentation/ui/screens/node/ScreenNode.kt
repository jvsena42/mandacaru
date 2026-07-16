package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import com.github.jvsena42.mandacaru.presentation.ui.components.ExpandableHeader
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.RequestNotificationPermissions
import com.github.jvsena42.mandacaru.presentation.utils.rememberAdaptiveLayout
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenNode(
    modifier: Modifier = Modifier,
    restartApplication: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    viewModel: NodeViewModel = koinViewModel()
) {
    RequestNotificationPermissions(onPermissionChange = {})

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentRestartApplication by rememberUpdatedState(restartApplication)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                NodeEvents.OnSnapshotApplied -> currentRestartApplication()
                is NodeEvents.OnShareAccumulator -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.payload)
                    }
                    context.startActivity(Intent.createChooser(share, null))
                }
            }
        }
    }

    val message = uiState.snapshotMessage
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnapshotMessage()
        }
    }

    var clipboardChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.ibd) {
        if (uiState.ibd && !clipboardChecked) {
            clipboardChecked = true
            viewModel.onCheckClipboardForImport(clipboard.getText()?.text)
        }
    }

    val hintPayload = uiState.clipboardImportPayload
    val hintMessage = stringResource(R.string.utreexo_clipboard_hint)
    val hintAction = stringResource(R.string.utreexo_clipboard_hint_action)
    LaunchedEffect(hintPayload) {
        if (hintPayload != null) {
            val result = snackbarHostState.showSnackbar(
                message = hintMessage,
                actionLabel = hintAction,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.onAcceptClipboardHint()
                SnackbarResult.Dismissed -> viewModel.onDismissClipboardHint()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomContentPadding),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        // Match the inner content's adaptive width/padding so the banner lines
        // up with the centered, width-capped layout on tablets instead of
        // spanning full-bleed above it.
        val layout = rememberAdaptiveLayout()
        val bannerHorizontalPadding = layout.horizontalPadding
        // The two-pane (tablet) dashboard fills the full pager width, while the
        // compact/medium layouts cap and center their content; mirror that so the
        // banner always spans the same width as the content beneath it.
        val bannerWidth = if (layout.useTwoPane) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .widthIn(max = layout.maxContentWidth)
                .fillMaxWidth()
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isWaitingForWifi) {
                Card(
                    modifier = bannerWidth
                        .padding(horizontal = bannerHorizontalPadding, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.waiting_for_wifi),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            ScreenNode(
                uiState = uiState,
                modifier = Modifier.weight(1f),
                bottomContentPadding = bottomContentPadding,
                onTogglePeers = viewModel::togglePeersExpanded,
                onToggleDiagnostics = viewModel::toggleDiagnosticsExpanded,
                onDisconnectPeer = viewModel::disconnectPeer,
                onPingPeers = viewModel::pingPeers,
                onClickScan = viewModel::onClickScan,
                onClickPaste = viewModel::onClickPaste,
                onDismissScanSheet = viewModel::onDismissScanSheet,
                onDismissPasteSheet = viewModel::onDismissPasteSheet,
                onPasteSheetTextChanged = viewModel::onPasteSheetTextChanged,
                onPasteFromClipboard = {
                    viewModel.onClickPasteFromClipboard(clipboard.getText()?.text)
                },
                onAccumulatorReceived = viewModel::onAccumulatorReceived,
                onDismissImportConfirm = viewModel::onDismissImportConfirm,
                onConfirmImport = viewModel::onConfirmImport,
                onToggleImportCard = viewModel::toggleImportCardExpanded,
                onToggleExportCard = viewModel::toggleExportCardExpanded,
                onClickShowExportQr = viewModel::onClickShowExportQr,
                onClickCopyExport = {
                    viewModel.onClickCopyExport()
                    uiState.exportPayload?.let { clipboard.setText(AnnotatedString(it)) }
                },
                onClickShareExport = viewModel::onClickShareExport,
                onDismissExportQrSheet = viewModel::onDismissExportQrSheet,
            )
        }
    }
}

@Composable
fun ScreenNode(
    uiState: NodeUiState,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    onTogglePeers: () -> Unit = {},
    onToggleDiagnostics: () -> Unit = {},
    onDisconnectPeer: (String) -> Unit = {},
    onPingPeers: () -> Unit = {},
    onClickScan: () -> Unit = {},
    onClickPaste: () -> Unit = {},
    onDismissScanSheet: () -> Unit = {},
    onDismissPasteSheet: () -> Unit = {},
    onPasteSheetTextChanged: (String) -> Unit = {},
    onPasteFromClipboard: () -> Unit = {},
    onAccumulatorReceived: (String) -> Unit = {},
    onDismissImportConfirm: () -> Unit = {},
    onConfirmImport: () -> Unit = {},
    onToggleImportCard: () -> Unit = {},
    onToggleExportCard: () -> Unit = {},
    onClickShowExportQr: () -> Unit = {},
    onClickCopyExport: () -> Unit = {},
    onClickShareExport: () -> Unit = {},
    onDismissExportQrSheet: () -> Unit = {},
) {
    var peerToDisconnect by remember { mutableStateOf<String?>(null) }
    var showPingConfirmation by remember { mutableStateOf(false) }

    val isHeaderSync = uiState.ibd && uiState.syncDecimal == 0f
    val isStalled = uiState.isStalled
    val headerSyncDecimal = uiState.headerSyncDecimal
    val filterSyncDecimal = uiState.filterSyncDecimal
    val isFilterSync = !isHeaderSync
            && !isStalled
            && uiState.syncDecimal >= 1f
            && filterSyncDecimal != null
            && filterSyncDecimal < 1f
    // Filters reaching the tip doesn't mean the wallet is scanned; while a
    // rescan runs the node is still finding the wallet's transactions, so we
    // surface that instead of claiming "Synced".
    val isWalletScanning = !isHeaderSync
            && !isStalled
            && uiState.syncDecimal >= 1f
            && (filterSyncDecimal == null || filterSyncDecimal >= 1f)
            && uiState.rescanInProgress
    val syncTitleRes = when {
        isHeaderSync -> R.string.syncing_headers_title
        isStalled -> R.string.sync_stalled_title
        isFilterSync -> R.string.syncing_filters_title
        uiState.ibd -> R.string.syncing_blocks_title
        isWalletScanning -> R.string.scanning_wallet_title
        else -> R.string.sync
    }

    ScreenNodeAlertDialogs(
        peerToDisconnect = peerToDisconnect,
        showPingConfirmation = showPingConfirmation,
        onClearPeerToDisconnect = { peerToDisconnect = null },
        onClearPingConfirmation = { showPingConfirmation = false },
        onDisconnectPeer = onDisconnectPeer,
        onPingPeers = onPingPeers,
    )

    ScreenNodeOverlays(
        uiState = uiState,
        onAccumulatorReceived = onAccumulatorReceived,
        onDismissScanSheet = onDismissScanSheet,
        onClickPaste = onClickPaste,
        onDismissPasteSheet = onDismissPasteSheet,
        onPasteSheetTextChanged = onPasteSheetTextChanged,
        onPasteFromClipboard = onPasteFromClipboard,
        onConfirmImport = onConfirmImport,
        onDismissImportConfirm = onDismissImportConfirm,
        onDismissExportQrSheet = onDismissExportQrSheet,
    )

    val layout = rememberAdaptiveLayout()
    val horizontalPadding = layout.horizontalPadding
    val maxContentWidth = layout.maxContentWidth
    val columns = if (layout.isMediumOrWider) {
        StaggeredGridCells.Adaptive(minSize = 360.dp)
    } else {
        StaggeredGridCells.Fixed(1)
    }
    val heroSpan = StaggeredGridItemSpan.FullLine

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (layout.useTwoPane) {
            TabletNodeDashboard(
                uiState = uiState,
                isHeaderSync = isHeaderSync,
                isFilterSync = isFilterSync,
                isStalled = isStalled,
                isWalletScanning = isWalletScanning,
                syncTitleRes = syncTitleRes,
                onPingClick = { showPingConfirmation = true },
                onRequestDisconnect = { peerToDisconnect = it },
                onClickScan = onClickScan,
                onClickPaste = onClickPaste,
                onToggleImportCard = onToggleImportCard,
                onToggleExportCard = onToggleExportCard,
                onClickShowExportQr = onClickShowExportQr,
                onClickCopyExport = onClickCopyExport,
                onClickShareExport = onClickShareExport,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1600.dp)
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = bottomContentPadding,
                    ),
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = columns,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = maxContentWidth),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = 16.dp,
                    end = horizontalPadding,
                    bottom = 16.dp + bottomContentPadding,
                ),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = heroSpan) { NodeTitle() }
                if (uiState.ibd && uiState.utreexoPeerCount == 0) {
                    item(span = heroSpan) { UtreexoWarningCard() }
                }
                if (isStalled) {
                    item(span = heroSpan) { SyncStalledWarningCard() }
                }
                item(span = heroSpan) {
                    SyncProgressCard(
                        titleRes = syncTitleRes,
                        isHeaderSync = isHeaderSync,
                        isFilterSync = isFilterSync,
                        isStalled = isStalled,
                        isWalletScanning = isWalletScanning,
                        headerSyncDecimal = headerSyncDecimal,
                        headerSyncPercentage = uiState.headerSyncPercentage,
                        filterSyncDecimal = filterSyncDecimal,
                        filterSyncPercentage = uiState.filterSyncPercentage,
                        syncPercentage = uiState.syncPercentage,
                        syncDecimal = uiState.syncDecimal,
                        rescanInProgress = uiState.rescanInProgress,
                        rescanProgressDecimal = uiState.rescanProgressDecimal,
                        rescanProgressPercentage = uiState.rescanProgressPercentage,
                        rescanBlocksProcessed = uiState.rescanBlocksProcessed,
                        rescanBlocksTotal = uiState.rescanBlocksTotal,
                    )
                }
                item { NetworkInfoCard(uiState = uiState) }
                if (uiState.ibd && !isHeaderSync && uiState.utreexoPeerCount > 0) {
                    item {
                        UtreexoImportCard(
                            isExpanded = uiState.isImportCardExpanded,
                            onToggle = onToggleImportCard,
                            onScanClick = onClickScan,
                            onPasteClick = onClickPaste,
                        )
                    }
                }
                item {
                    PeersCard(
                        uiState = uiState,
                        onTogglePeers = onTogglePeers,
                        onPingClick = { showPingConfirmation = true },
                        onRequestDisconnect = { peerToDisconnect = it },
                    )
                }
                if (!uiState.ibd && uiState.syncDecimal >= 1f && uiState.utreexoPeerCount > 0) {
                    item {
                        UtreexoExportCard(
                            isExpanded = uiState.isExportCardExpanded,
                            onToggle = onToggleExportCard,
                            onShowQrClick = onClickShowExportQr,
                            onCopyClick = onClickCopyExport,
                            onShareClick = onClickShareExport,
                        )
                    }
                }
                if (uiState.enableAdvancedFeatures) {
                    item {
                        DiagnosticsCard(
                            uiState = uiState,
                            onToggle = onToggleDiagnostics,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenNodeAlertDialogs(
    peerToDisconnect: String?,
    showPingConfirmation: Boolean,
    onClearPeerToDisconnect: () -> Unit,
    onClearPingConfirmation: () -> Unit,
    onDisconnectPeer: (String) -> Unit,
    onPingPeers: () -> Unit,
) {
    peerToDisconnect?.let { address ->
        AlertDialog(
            onDismissRequest = onClearPeerToDisconnect,
            title = { Text("Disconnect Peer") },
            text = { Text("Disconnect from $address?") },
            confirmButton = {
                TextButton(onClick = {
                    onDisconnectPeer(address)
                    onClearPeerToDisconnect()
                }) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearPeerToDisconnect) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPingConfirmation) {
        AlertDialog(
            onDismissRequest = onClearPingConfirmation,
            title = { Text("Ping All Peers") },
            text = { Text("Send a ping to all connected peers?") },
            confirmButton = {
                TextButton(onClick = {
                    onPingPeers()
                    onClearPingConfirmation()
                }) {
                    Text("Ping")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearPingConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScreenNodeOverlays(
    uiState: NodeUiState,
    onAccumulatorReceived: (String) -> Unit,
    onDismissScanSheet: () -> Unit,
    onClickPaste: () -> Unit,
    onDismissPasteSheet: () -> Unit,
    onPasteSheetTextChanged: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImportConfirm: () -> Unit,
    onDismissExportQrSheet: () -> Unit,
) {
    if (uiState.ibd && uiState.isScanSheetOpen) {
        UtreexoScanSheet(
            onPayloadScanned = onAccumulatorReceived,
            onDismiss = onDismissScanSheet,
            onPasteFallback = {
                onDismissScanSheet()
                onClickPaste()
            },
        )
    }
    if (uiState.ibd && uiState.isPasteSheetOpen) {
        UtreexoPasteSheet(
            text = uiState.pasteSheetText,
            errorMessage = uiState.pasteSheetError,
            onTextChange = onPasteSheetTextChanged,
            onPasteFromClipboard = onPasteFromClipboard,
            onPayloadSubmitted = { onAccumulatorReceived(uiState.pasteSheetText.trim()) },
            onDismiss = onDismissPasteSheet,
        )
    }
    val preview = uiState.pendingSnapshotPreview
    if (uiState.ibd && preview != null) {
        UtreexoImportConfirmDialog(
            preview = preview,
            onConfirm = onConfirmImport,
            onDismiss = onDismissImportConfirm,
        )
    }
    val exportForQr = uiState.exportPayload
    if (!uiState.ibd && uiState.isExportQrSheetOpen && exportForQr != null) {
        UtreexoExportQrSheet(
            payload = exportForQr,
            onDismiss = onDismissExportQrSheet,
        )
    }

    if (uiState.isApplyingSnapshot) {
        ApplyingSnapshotOverlay()
    }
}

@Composable
private fun NodeTitle() {
    Text(
        stringResource(R.string.node),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
internal fun NetworkInfoCard(uiState: NodeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Network Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            InfoRow(
                label = stringResource(R.string.network),
                value = uiState.network,
                valueTestTag = "node_network",
                icon = {
                    Icon(
                        Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            InfoRow(
                label = stringResource(R.string.number_of_peers),
                value = uiState.numberOfPeers,
                valueTestTag = "node_peer_count",
                icon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
            )

            InfoRow(
                label = stringResource(R.string.difficulty),
                value = uiState.difficulty,
                valueTestTag = "node_difficulty",
                icon = {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

@Composable
private fun PeersCard(
    uiState: NodeUiState,
    onTogglePeers: () -> Unit,
    onPingClick: () -> Unit,
    onRequestDisconnect: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpandableHeader(
                title = "Peers (${uiState.numberOfPeers.ifEmpty { "0" }})",
                icon = Icons.Outlined.Hub,
                isExpanded = uiState.isPeersExpanded,
                onToggle = onTogglePeers
            )

            AnimatedVisibility(
                visible = uiState.isPeersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.peers.isNotEmpty()) {
                        TextButton(onClick = onPingClick) {
                            Icon(
                                Icons.Outlined.NetworkPing,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping All")
                        }
                    }

                    if (uiState.peers.isEmpty()) {
                        Text(
                            "No peers connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.peers.forEachIndexed { index, peer ->
                            PeerItem(
                                peer = peer,
                                onDisconnect = { onRequestDisconnect(peer.peer.address) },
                                showAdvancedDetails = uiState.enableAdvancedFeatures,
                            )
                            if (index < uiState.peers.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    uiState: NodeUiState,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpandableHeader(
                title = stringResource(R.string.diagnostics),
                icon = Icons.Outlined.Info,
                isExpanded = uiState.isDiagnosticsExpanded,
                onToggle = onToggle
            )

            AnimatedVisibility(
                visible = uiState.isDiagnosticsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoRow(
                        label = stringResource(R.string.uptime),
                        value = uiState.uptime,
                    )
                }
            }
        }
    }
}

private data class SyncStepStates(
    val headers: StepState,
    val blocks: StepState,
    val filters: StepState?,
    val allDone: Boolean,
)

private fun blocksStepState(
    headersDone: Boolean,
    blocksDone: Boolean,
    walletScanning: Boolean,
    hasFiltersStep: Boolean,
): StepState = when {
    walletScanning && !hasFiltersStep -> StepState.Current
    blocksDone -> StepState.Done
    headersDone -> StepState.Current
    else -> StepState.Pending
}

private fun filtersStepState(
    hasFiltersStep: Boolean,
    walletScanning: Boolean,
    filtersDownloaded: Boolean,
    blocksDone: Boolean,
): StepState? = when {
    !hasFiltersStep -> null
    walletScanning -> StepState.Current
    filtersDownloaded && blocksDone -> StepState.Done
    blocksDone -> StepState.Current
    else -> StepState.Pending
}

private fun computeSyncStepStates(
    isHeaderSync: Boolean,
    syncDecimal: Float,
    filterSyncDecimal: Float?,
    rescanInProgress: Boolean,
): SyncStepStates {
    val hasFiltersStep = filterSyncDecimal != null
    val headersDone = !isHeaderSync
    val blocksDone = syncDecimal >= 1f
    val filtersDownloaded = filterSyncDecimal == null || filterSyncDecimal >= 1f
    // A running rescan means the wallet isn't fully scanned yet: keep the last
    // step Current and don't report everything done.
    val walletScanning = headersDone && blocksDone && filtersDownloaded && rescanInProgress
    val headers = if (headersDone) StepState.Done else StepState.Current
    val blocks = blocksStepState(headersDone, blocksDone, walletScanning, hasFiltersStep)
    val filters = filtersStepState(hasFiltersStep, walletScanning, filtersDownloaded, blocksDone)
    val allDone = headersDone && blocksDone && filtersDownloaded && !rescanInProgress
    return SyncStepStates(headers, blocks, filters, allDone)
}

private data class SyncProgressInputs(
    val isStalled: Boolean,
    val isHeaderSync: Boolean,
    val isFilterSync: Boolean,
    val isWalletScanning: Boolean,
    val headerSyncDecimal: Float?,
    val headerSyncPercentage: String,
    val filterSyncDecimal: Float?,
    val filterSyncPercentage: String,
    val syncPercentage: String,
    val syncDecimal: Float,
    val rescanProgressDecimal: Float?,
    val rescanProgressPercentage: String,
)

private fun SyncProgressInputs.rawDecimal(): Float? = when {
    isStalled -> 0f
    isHeaderSync && headerSyncDecimal != null -> headerSyncDecimal
    isHeaderSync -> null
    isFilterSync && filterSyncDecimal != null -> filterSyncDecimal
    // Use the real rescan progress when the daemon reports a block total;
    // otherwise show an indeterminate bar rather than a misleading 100%.
    isWalletScanning -> rescanProgressDecimal
    else -> syncDecimal
}

private fun SyncProgressInputs.percentageText(): String? = when {
    isStalled -> null
    isHeaderSync && headerSyncDecimal != null -> "$headerSyncPercentage%"
    isHeaderSync -> null
    isFilterSync && filterSyncDecimal != null -> "$filterSyncPercentage%"
    isWalletScanning -> rescanProgressDecimal?.let { "$rescanProgressPercentage%" }
    else -> "$syncPercentage%"
}

@Composable
private fun SyncProgressBar(
    isStalled: Boolean,
    rawDecimal: Float?,
    animatedDecimal: Float,
) {
    val barModifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
    when {
        isStalled -> LinearProgressIndicator(
            progress = { 0f },
            modifier = barModifier,
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        rawDecimal == null -> LinearProgressIndicator(
            modifier = barModifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        else -> LinearProgressIndicator(
            progress = { animatedDecimal },
            modifier = barModifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun SyncProgressTitleRow(titleRes: Int, percentageText: String?) {
    AnimatedContent(
        targetState = titleRes,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "syncTitle",
    ) { currentTitleRes ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(currentTitleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (percentageText != null) {
                Text(
                    percentageText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("node_sync_percentage"),
                )
            }
        }
    }
}

@Composable
private fun SyncProgressCard(
    titleRes: Int,
    isHeaderSync: Boolean,
    isFilterSync: Boolean,
    isStalled: Boolean,
    isWalletScanning: Boolean,
    headerSyncDecimal: Float?,
    headerSyncPercentage: String,
    filterSyncDecimal: Float?,
    filterSyncPercentage: String,
    syncPercentage: String,
    syncDecimal: Float,
    rescanInProgress: Boolean,
    rescanProgressDecimal: Float?,
    rescanProgressPercentage: String,
    rescanBlocksProcessed: Int,
    rescanBlocksTotal: Int,
) {
    val inputs = SyncProgressInputs(
        isStalled = isStalled,
        isHeaderSync = isHeaderSync,
        isFilterSync = isFilterSync,
        isWalletScanning = isWalletScanning,
        headerSyncDecimal = headerSyncDecimal,
        headerSyncPercentage = headerSyncPercentage,
        filterSyncDecimal = filterSyncDecimal,
        filterSyncPercentage = filterSyncPercentage,
        syncPercentage = syncPercentage,
        syncDecimal = syncDecimal,
        rescanProgressDecimal = rescanProgressDecimal,
        rescanProgressPercentage = rescanProgressPercentage,
    )
    val rawDecimal = inputs.rawDecimal()
    val animatedDecimal by animateFloatAsState(
        targetValue = rawDecimal ?: 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "syncProgress",
    )
    val percentageText = inputs.percentageText()
    val steps =
        computeSyncStepStates(isHeaderSync, syncDecimal, filterSyncDecimal, rescanInProgress)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            AnimatedVisibility(
                visible = !isStalled && !steps.allDone,
                enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) +
                        shrinkVertically(animationSpec = tween(300)),
            ) {
                Column {
                    SyncStepper(
                        headersState = steps.headers,
                        blocksState = steps.blocks,
                        filtersState = steps.filters,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            SyncProgressTitleRow(titleRes = titleRes, percentageText = percentageText)

            Spacer(modifier = Modifier.height(12.dp))

            SyncProgressBar(
                isStalled = isStalled,
                rawDecimal = rawDecimal,
                animatedDecimal = animatedDecimal,
            )

            if (isWalletScanning && rescanBlocksTotal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val numberFormat = remember { NumberFormat.getNumberInstance() }
                Text(
                    stringResource(
                        R.string.scanning_wallet_blocks,
                        numberFormat.format(rescanBlocksProcessed),
                        numberFormat.format(rescanBlocksTotal),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

internal enum class StepState { Done, Current, Pending }

@Composable
internal fun SyncStepper(
    headersState: StepState,
    blocksState: StepState,
    filtersState: StepState?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        SyncStepNode(
            label = stringResource(R.string.sync_step_headers),
            state = headersState,
        )
        SyncStepConnector(
            filled = headersState == StepState.Done,
            modifier = Modifier
                .weight(1f)
                .padding(top = 9.dp, start = 4.dp, end = 4.dp),
        )
        SyncStepNode(
            label = stringResource(R.string.sync_step_blocks),
            state = blocksState,
        )
        if (filtersState != null) {
            SyncStepConnector(
                filled = blocksState == StepState.Done,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 9.dp, start = 4.dp, end = 4.dp),
            )
            SyncStepNode(
                label = stringResource(R.string.sync_step_filters),
                state = filtersState,
            )
        }
    }
}

@Composable
private fun SyncStepNode(label: String, state: StepState) {
    val activeColor = MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val pendingColor = activeColor.copy(alpha = 0.35f)

    val dotFillColor by animateColorAsState(
        targetValue = when (state) {
            StepState.Done, StepState.Current -> activeColor
            StepState.Pending -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "stepDotFill",
    )
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            StepState.Done, StepState.Current -> activeColor
            StepState.Pending -> pendingColor
        },
        animationSpec = tween(300),
        label = "stepBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = when (state) {
            StepState.Done, StepState.Current -> activeColor
            StepState.Pending -> pendingColor
        },
        animationSpec = tween(300),
        label = "stepLabel",
    )

    val pulseScale = if (state == StepState.Current) {
        val transition = rememberInfiniteTransition(label = "stepPulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "stepPulseScale",
        )
        scale
    } else {
        1f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val checkAlpha by animateFloatAsState(
            targetValue = if (state == StepState.Done) 1f else 0f,
            animationSpec = tween(200),
            label = "stepCheckAlpha",
        )
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(20.dp)
                .clip(CircleShape)
                .background(dotFillColor)
                .border(width = 1.5.dp, color = borderColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier
                    .size(14.dp)
                    .alpha(checkAlpha),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
    }
}

@Composable
private fun SyncStepConnector(filled: Boolean, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.onPrimaryContainer
    val inactiveColor = activeColor.copy(alpha = 0.25f)
    val color by animateColorAsState(
        targetValue = if (filled) activeColor else inactiveColor,
        animationSpec = tween(300),
        label = "stepConnector",
    )
    Box(
        modifier = modifier
            .height(2.dp)
            .background(color),
    )
}

@Composable
internal fun SyncStalledWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.sync_stalled_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.sync_stalled_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ApplyingSnapshotOverlay() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.utreexo_imported_restarting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun UtreexoWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.utreexo_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.utreexo_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
internal fun PeerItem(
    peer: PeerUi,
    onDisconnect: () -> Unit = {},
    showAdvancedDetails: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeerFlag(countryCode = peer.countryCode, flag = peer.flag)

            Text(
                peer.peer.address,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("node_disconnect_peer")
            ) {
                Icon(
                    Icons.Outlined.LinkOff,
                    contentDescription = "Disconnect peer",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Text(
            peer.peer.userAgent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                peer.peer.services
                    .removePrefix("ServiceFlags(")
                    .removeSuffix(")"),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        if (showAdvancedDetails) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeerChip(peer.peer.state)
                PeerChip(peer.peer.kind)
            }
        }
    }
}


@Composable
private fun PeerChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueTestTag: String? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon?.invoke()
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier,
        )
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    MandacaruTheme {
        Surface {
            ScreenNode(
                NodeUiState(
                    numberOfPeers = "5",
                    blockHeight = "1,235,334",
                    blockHash = "00000cb40a568e8da8a045ced110137e159f890ac4da883b6b17dc651b3a8049",
                    network = "Signet",
                    difficulty = "138.97 T",
                    syncPercentage = "78.00",
                    syncDecimal = 0.78f,
                    ibd = true,
                    utreexoPeerCount = 0,
                    isPeersExpanded = true,
                    uptime = "2d 5h 32m 10s",
                    isDiagnosticsExpanded = true,
                    peers = listOf(
                        PeerInfoResult(
                            address = "194.145.199.26:8333",
                            initialHeight = 943609,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|NETWORK_LIMITED|P2P_V2)",
                            state = "Ready",
                            userAgent = "/Satoshi:30.0.0/"
                        ).withCountry("UA"),
                        PeerInfoResult(
                            address = "59.3.9.212:8333",
                            initialHeight = 943609,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|NETWORK_LIMITED|P2P_V2)",
                            state = "Ready",
                            userAgent = "/Satoshi:28.1.0/"
                        ).withCountry("KR")
                    )
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StalledPreview() {
    MandacaruTheme {
        Surface {
            ScreenNode(
                NodeUiState(
                    numberOfPeers = "10",
                    blockHeight = "17,904",
                    headerHeightRaw = 17_904,
                    blockHash = "0000000000000ed5f3f1d61f1bbf73c89c2cc01dec02e2fa7eaa9f6cabf2a7df",
                    network = "BITCOIN",
                    difficulty = "1.00",
                    syncPercentage = "100.00",
                    syncDecimal = 1f,
                    ibd = false,
                    isStalled = true,
                    utreexoPeerCount = 1,
                    uptime = "14h 02m",
                    peers = listOf(
                        PeerInfoResult(
                            address = "194.145.199.26:8333",
                            initialHeight = 947_390,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|UTREEXO)",
                            state = "Ready",
                            userAgent = "/Satoshi:30.0.0/"
                        ).withCountry("UA"),
                    )
                )
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Tablet scanning", widthDp = 1280, heightDp = 840)
@Composable
private fun WalletScanningPreview() {
    MandacaruTheme {
        Surface {
            ScreenNode(
                NodeUiState(
                    numberOfPeers = "8",
                    blockHeight = "800,000",
                    headerHeightRaw = 800_000,
                    blockHash = "00000cb40a568e8da8a045ced110137e159f890ac4da883b6b17dc651b3a8049",
                    network = "BITCOIN",
                    difficulty = "138.97 T",
                    syncPercentage = "100.00",
                    syncDecimal = 1f,
                    filterSyncDecimal = 1f,
                    filterSyncPercentage = "100.00",
                    ibd = false,
                    rescanInProgress = true,
                    rescanProgressDecimal = 0.42f,
                    rescanProgressPercentage = "42.00",
                    rescanBlocksProcessed = 336_000,
                    rescanBlocksTotal = 800_000,
                    utreexoPeerCount = 1,
                    uptime = "14h 02m",
                    peers = listOf(
                        PeerInfoResult(
                            address = "194.145.199.26:8333",
                            initialHeight = 947_390,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|UTREEXO)",
                            state = "Ready",
                            userAgent = "/Satoshi:30.0.0/"
                        ).withCountry("UA"),
                    )
                )
            )
        }
    }
}

@Preview(name = "Tablet", widthDp = 840, heightDp = 1280)
@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 840)
@Composable
private fun TabletPreview() {
    MandacaruTheme {
        Surface {
            ScreenNode(
                NodeUiState(
                    numberOfPeers = "8",
                    blockHeight = "1,235,334",
                    blockHash = "00000cb40a568e8da8a045ced110137e159f890ac4da883b6b17dc651b3a8049",
                    network = "Signet",
                    difficulty = "138.97 T",
                    syncPercentage = "78.00",
                    syncDecimal = 0.78f,
                    ibd = true,
                    utreexoPeerCount = 2,
                    isPeersExpanded = true,
                    uptime = "2d 5h 32m 10s",
                    isDiagnosticsExpanded = true,
                    peers = listOf(
                        PeerInfoResult(
                            address = "194.145.199.26:8333",
                            initialHeight = 943609,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|NETWORK_LIMITED|P2P_V2)",
                            state = "Ready",
                            userAgent = "/Satoshi:30.0.0/"
                        ).withCountry("UA"),
                        PeerInfoResult(
                            address = "59.3.9.212:8333",
                            initialHeight = 943609,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|NETWORK_LIMITED|P2P_V2)",
                            state = "Ready",
                            userAgent = "/Satoshi:28.1.0/"
                        ).withCountry("KR")
                    )
                )
            )
        }
    }
}
