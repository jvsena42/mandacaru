package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R

@Composable
internal fun TabletNodeDashboard(
    uiState: NodeUiState,
    isHeaderSync: Boolean,
    isFilterSync: Boolean,
    isStalled: Boolean,
    syncTitleRes: Int,
    onPingClick: () -> Unit,
    onRequestDisconnect: (String) -> Unit,
    onClickScan: () -> Unit,
    onClickPaste: () -> Unit,
    onToggleImportCard: () -> Unit,
    onToggleExportCard: () -> Unit,
    onClickShowExportQr: () -> Unit,
    onClickCopyExport: () -> Unit,
    onClickShareExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.ibd && uiState.utreexoPeerCount == 0) {
            UtreexoWarningCard()
        }
        if (isStalled) {
            SyncStalledWarningCard()
        }

        HeroStatusBand(
            uiState = uiState,
            isHeaderSync = isHeaderSync,
            isFilterSync = isFilterSync,
            isStalled = isStalled,
            syncTitleRes = syncTitleRes,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BlocksPlaceholderCard(
                    blockHash = uiState.blockHash,
                    blockHeight = uiState.blockHeight,
                )
                if (uiState.ibd && !isHeaderSync && uiState.utreexoPeerCount > 0) {
                    UtreexoImportCard(
                        isExpanded = uiState.isImportCardExpanded,
                        onToggle = onToggleImportCard,
                        onScanClick = onClickScan,
                        onPasteClick = onClickPaste,
                    )
                }
                if (!uiState.ibd && uiState.syncDecimal >= 1f && uiState.utreexoPeerCount > 0) {
                    UtreexoExportCard(
                        isExpanded = uiState.isExportCardExpanded,
                        onToggle = onToggleExportCard,
                        onShowQrClick = onClickShowExportQr,
                        onCopyClick = onClickCopyExport,
                        onShareClick = onClickShareExport,
                    )
                }
                NetworkInfoCard(uiState = uiState)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TabletPeersCard(
                    uiState = uiState,
                    onPingClick = onPingClick,
                    onRequestDisconnect = onRequestDisconnect,
                )
                TabletDiagnosticsCard(uiState = uiState)
            }
        }
    }
}

@Composable
private fun HeroStatusBand(
    uiState: NodeUiState,
    isHeaderSync: Boolean,
    isFilterSync: Boolean,
    isStalled: Boolean,
    syncTitleRes: Int,
) {
    val rawDecimal: Float? = when {
        isStalled -> 0f
        isHeaderSync && uiState.headerSyncDecimal != null -> uiState.headerSyncDecimal
        isHeaderSync -> null
        isFilterSync && uiState.filterSyncDecimal != null -> uiState.filterSyncDecimal
        else -> uiState.syncDecimal
    }
    val animatedDecimal by animateFloatAsState(
        targetValue = rawDecimal ?: 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "heroSyncProgress",
    )

    val percentageText: String = when {
        isStalled -> "—"
        isHeaderSync && uiState.headerSyncDecimal != null -> "${uiState.headerSyncPercentage}%"
        isHeaderSync -> "—"
        isFilterSync && uiState.filterSyncDecimal != null -> "${uiState.filterSyncPercentage}%"
        else -> "${uiState.syncPercentage}%"
    }

    val hasFiltersStep = uiState.filterSyncDecimal != null
    val headersDone = !isHeaderSync
    val blocksDone = uiState.syncDecimal >= 1f
    val filtersDone = uiState.filterSyncDecimal == null || uiState.filterSyncDecimal >= 1f
    val allDone = headersDone && blocksDone && filtersDone
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigSyncRing(
                progress = animatedDecimal,
                indeterminate = rawDecimal == null,
                isStalled = isStalled,
                percentageText = percentageText,
                allDone = allDone,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(syncTitleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                AnimatedVisibility(
                    visible = !isStalled && !allDone,
                    enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) +
                        shrinkVertically(animationSpec = tween(300)),
                ) {
                    SyncStepper(
                        headersState = headersState,
                        blocksState = blocksState,
                        filtersState = filtersState,
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    KpiTile(
                        label = stringResource(R.string.network),
                        value = uiState.network,
                        modifier = Modifier.weight(1f),
                    )
                    KpiTile(
                        label = stringResource(R.string.block_height),
                        value = uiState.blockHeight,
                        modifier = Modifier.weight(1f),
                    )
                    KpiTile(
                        label = stringResource(R.string.difficulty),
                        value = uiState.difficulty,
                        modifier = Modifier.weight(1f),
                    )
                    KpiTile(
                        label = stringResource(R.string.uptime),
                        value = uiState.uptime,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BigSyncRing(
    progress: Float,
    indeterminate: Boolean,
    isStalled: Boolean,
    percentageText: String,
    allDone: Boolean,
) {
    val ringColor = when {
        isStalled -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
    val ringSize = 200.dp
    val strokeWidth = 14.dp
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        if (indeterminate) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (allDone) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.synced),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Text(
                    percentageText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value.ifEmpty { "—" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BlocksPlaceholderCard(
    blockHash: String,
    blockHeight: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.recent_blocks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        stringResource(R.string.coming_soon),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(8) { idx ->
                        val tone = 0.65f - idx * 0.06f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = tone.coerceAtLeast(0.12f),
                                    ),
                                ),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (blockHeight.isNotEmpty()) {
                InfoRow(
                    label = stringResource(R.string.block_height),
                    value = blockHeight,
                )
            }
            if (blockHash.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.latest_block_hash),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        blockHash,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                stringResource(R.string.block_explorer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TabletPeersCard(
    uiState: NodeUiState,
    onPingClick: () -> Unit,
    onRequestDisconnect: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Hub,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Peers (${uiState.numberOfPeers.ifEmpty { "0" }})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (uiState.peers.isNotEmpty()) {
                    TextButton(onClick = onPingClick) {
                        Icon(
                            Icons.Outlined.NetworkPing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping All")
                    }
                }
            }

            if (uiState.peers.isEmpty()) {
                Text(
                    "No peers connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.peers.forEachIndexed { index, peer ->
                        PeerItem(
                            peer = peer,
                            onDisconnect = { onRequestDisconnect(peer.address) },
                        )
                        if (index < uiState.peers.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletDiagnosticsCard(uiState: NodeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.diagnostics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            InfoRow(
                label = stringResource(R.string.uptime),
                value = uiState.uptime,
            )
        }
    }
}
