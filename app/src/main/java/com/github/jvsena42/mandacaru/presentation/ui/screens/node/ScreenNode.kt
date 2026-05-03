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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import com.github.jvsena42.mandacaru.presentation.ui.components.ExpandableHeader
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.RequestNotificationPermissions
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenNode(
    restartApplication: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    viewModel: NodeViewModel = koinViewModel()
) {
    RequestNotificationPermissions(onPermissionChange = {})

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                NodeEvents.OnSnapshotApplied -> restartApplication()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        ScreenNode(
            uiState = uiState,
            modifier = Modifier.padding(padding),
            bottomContentPadding = bottomContentPadding,
            onTogglePeers = viewModel::togglePeersExpanded,
            onToggleDiagnostics = viewModel::toggleDiagnosticsExpanded,
            onDisconnectPeer = viewModel::disconnectPeer,
            onPingPeers = viewModel::pingPeers,
            onClickScan = viewModel::onClickScan,
            onClickPaste = viewModel::onClickPaste,
            onDismissScanSheet = viewModel::onDismissScanSheet,
            onDismissPasteSheet = viewModel::onDismissPasteSheet,
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
    val syncTitleRes = when {
        isHeaderSync -> R.string.syncing_headers_title
        isStalled -> R.string.sync_stalled_title
        isFilterSync -> R.string.syncing_filters_title
        uiState.ibd -> R.string.syncing_blocks_title
        else -> R.string.sync
    }

    peerToDisconnect?.let { address ->
        AlertDialog(
            onDismissRequest = { peerToDisconnect = null },
            title = { Text("Disconnect Peer") },
            text = { Text("Disconnect from $address?") },
            confirmButton = {
                TextButton(onClick = {
                    onDisconnectPeer(address)
                    peerToDisconnect = null
                }) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { peerToDisconnect = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPingConfirmation) {
        AlertDialog(
            onDismissRequest = { showPingConfirmation = false },
            title = { Text("Ping All Peers") },
            text = { Text("Send a ping to all connected peers?") },
            confirmButton = {
                TextButton(onClick = {
                    onPingPeers()
                    showPingConfirmation = false
                }) {
                    Text("Ping")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPingConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            onPayloadSubmitted = onAccumulatorReceived,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp + bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
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
        if (uiState.ibd && uiState.utreexoPeerCount == 0) {
            item { UtreexoWarningCard() }
        }
        if (isStalled) {
            item { SyncStalledWarningCard() }
        }
        item {
            SyncProgressCard(
                titleRes = syncTitleRes,
                isHeaderSync = isHeaderSync,
                isFilterSync = isFilterSync,
                isStalled = isStalled,
                headerSyncDecimal = headerSyncDecimal,
                headerSyncPercentage = uiState.headerSyncPercentage,
                filterSyncDecimal = filterSyncDecimal,
                filterSyncPercentage = uiState.filterSyncPercentage,
                syncPercentage = uiState.syncPercentage,
                syncDecimal = uiState.syncDecimal,
            )
        }

        // Network Info Card
        item {
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

        // Peers Info Card
        item {
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
                                TextButton(onClick = { showPingConfirmation = true }) {
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
                                        onDisconnect = { peerToDisconnect = peer.address }
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

        // Diagnostics Card
        item {
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
                        onToggle = onToggleDiagnostics
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
    }
}

@Composable
private fun SyncProgressCard(
    titleRes: Int,
    isHeaderSync: Boolean,
    isFilterSync: Boolean,
    isStalled: Boolean,
    headerSyncDecimal: Float?,
    headerSyncPercentage: String,
    filterSyncDecimal: Float?,
    filterSyncPercentage: String,
    syncPercentage: String,
    syncDecimal: Float,
) {
    val rawDecimal: Float? = when {
        isStalled -> 0f
        isHeaderSync && headerSyncDecimal != null -> headerSyncDecimal
        isHeaderSync -> null
        isFilterSync && filterSyncDecimal != null -> filterSyncDecimal
        else -> syncDecimal
    }
    val animatedDecimal by animateFloatAsState(
        targetValue = rawDecimal ?: 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "syncProgress",
    )

    val percentageText: String? = when {
        isStalled -> null
        isHeaderSync && headerSyncDecimal != null -> "$headerSyncPercentage%"
        isHeaderSync -> null
        isFilterSync && filterSyncDecimal != null -> "$filterSyncPercentage%"
        else -> "$syncPercentage%"
    }

    val hasFiltersStep = filterSyncDecimal != null
    val headersDone = !isHeaderSync
    val blocksDone = syncDecimal >= 1f
    val filtersDone = filterSyncDecimal == null || filterSyncDecimal >= 1f
    val allStepsDone = headersDone && blocksDone && filtersDone
    val headersState = if (headersDone) StepState.Done else StepState.Current
    val blocksState = when {
        blocksDone -> StepState.Done
        headersDone -> StepState.Current
        else -> StepState.Pending
    }
    val filtersState: StepState? = when {
        !hasFiltersStep -> null
        filtersDone && blocksDone -> StepState.Done
        blocksDone -> StepState.Current
        else -> StepState.Pending
    }

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
                visible = !isStalled && !allStepsDone,
                enter = fadeIn(animationSpec = tween(300)) +
                    expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) +
                    shrinkVertically(animationSpec = tween(300)),
            ) {
                Column {
                    SyncStepper(
                        headersState = headersState,
                        blocksState = blocksState,
                        filtersState = filtersState,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

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
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
    }
}

private enum class StepState { Done, Current, Pending }

@Composable
private fun SyncStepper(
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
private fun SyncStalledWarningCard() {
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
private fun UtreexoWarningCard() {
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
private fun PeerItem(
    peer: PeerInfoResult,
    onDisconnect: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                peer.address,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier.size(32.dp)
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
            peer.userAgent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                peer.services
                    .removePrefix("ServiceFlags(")
                    .removeSuffix(")"),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeerChip(peer.state)
            PeerChip(peer.kind)
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
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
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
            color = MaterialTheme.colorScheme.onSurface
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
                        ),
                        PeerInfoResult(
                            address = "59.3.9.212:8333",
                            initialHeight = 943609,
                            kind = "regular",
                            services = "ServiceFlags(NETWORK|WITNESS|COMPACT_FILTERS|NETWORK_LIMITED|P2P_V2)",
                            state = "Ready",
                            userAgent = "/Satoshi:28.1.0/"
                        )
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
                        ),
                    )
                )
            )
        }
    }
}
