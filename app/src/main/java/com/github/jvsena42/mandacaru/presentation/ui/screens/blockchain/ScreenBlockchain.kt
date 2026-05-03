package com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.BlockHeaderResult
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenBlockchain(
    bottomContentPadding: Dp = 0.dp,
    viewModel: BlockchainViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenBlockchainContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        bottomContentPadding = bottomContentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenBlockchainContent(
    uiState: BlockchainUiState,
    onAction: (BlockchainAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            scope.launch {
                snackBarHostState.showSnackbar(message = uiState.errorMessage)
                currentOnAction(BlockchainAction.ClearSnackBarMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { contentPadding ->
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val isMediumOrWider = windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        )
        val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        )
        val horizontalPadding = when {
            isExpandedWidth -> 32.dp
            isMediumOrWider -> 24.dp
            else -> 16.dp
        }
        val maxContentWidth = if (isMediumOrWider) 1200.dp else 600.dp
        val columns = if (isMediumOrWider) {
            StaggeredGridCells.Adaptive(minSize = 360.dp)
        } else {
            StaggeredGridCells.Fixed(1)
        }
        val heroSpan = StaggeredGridItemSpan.FullLine

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyVerticalStaggeredGrid(
                columns = columns,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = maxContentWidth),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = 0.dp,
                    end = horizontalPadding,
                    bottom = 16.dp + bottomContentPadding,
                ),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = heroSpan) { BlockchainTitle() }
                item(span = heroSpan) {
                    AnimatedVisibility(visible = uiState.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                }
                item { ChainStatusCard(uiState = uiState, onAction = onAction) }
                item {
                    SearchCard(
                        uiState = uiState,
                        onAction = onAction,
                        focusManager = focusManager,
                    )
                }
                if (uiState.blockHeader != null) {
                    item {
                        BlockHeaderCard(
                            header = uiState.blockHeader,
                            blockHash = uiState.blockHash,
                            blockHeight = uiState.blockHeight,
                        )
                    }
                }
                if (uiState.blockHeader == null
                    && !uiState.isLoading
                    && uiState.searchQuery.isEmpty()
                ) {
                    item(span = heroSpan) { EmptyExploreState() }
                }
            }
        }
    }
}

@Composable
private fun BlockchainTitle() {
    Text(
        stringResource(R.string.blockchain),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ChainStatusCard(
    uiState: BlockchainUiState,
    onAction: (BlockchainAction) -> Unit,
) {
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.chain_status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.block_height),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    uiState.blockCount.ifEmpty { "..." },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.validated_blocks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    uiState.validatedBlocks.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (uiState.bestBlockHash.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.best_block),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        uiState.bestBlockHash,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = { onAction(BlockchainAction.SearchLatestBlock) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.bestBlockHash.isNotEmpty(),
            ) {
                Text(stringResource(R.string.view_latest_block))
            }
        }
    }
}

@Composable
private fun SearchCard(
    uiState: BlockchainUiState,
    onAction: (BlockchainAction) -> Unit,
    focusManager: FocusManager,
) {
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
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.block_lookup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                enabled = !uiState.isLoading,
                onValueChange = { newText ->
                    onAction(BlockchainAction.OnSearchChanged(newText.trim()))
                },
                label = { Text(stringResource(R.string.search_block_hint)) },
                placeholder = { Text(stringResource(R.string.search_block_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                stringResource(R.string.search_block_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyExploreState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Tag,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.explore_blocks),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.explore_blocks_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BlockHeaderCard(
    header: BlockHeaderResult,
    blockHash: String,
    blockHeight: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    stringResource(R.string.block_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (blockHeight.isNotEmpty()) {
                BlockDetailRow(
                    label = stringResource(R.string.block_height),
                    value = blockHeight
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            BlockDetailRow(
                label = stringResource(R.string.block_hash),
                value = blockHash,
                isMonospace = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.block_time),
                value = BlockchainViewModel.formatBlockTime(header.time)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.block_version),
                value = header.version.toString()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.merkle_root),
                value = header.merkleRoot,
                isMonospace = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.previous_block),
                value = header.prevBlockhash,
                isMonospace = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.nonce),
                value = header.nonce.toString()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            BlockDetailRow(
                label = stringResource(R.string.bits),
                value = header.bits.toString()
            )
        }
    }
}

@Composable
private fun BlockDetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .padding(8.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    MandacaruTheme {
        Surface {
            ScreenBlockchainContent(
                uiState = BlockchainUiState(
                    blockCount = "943,609",
                    bestBlockHash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                    validatedBlocks = 943609,
                    blockHeader = BlockHeaderResult(
                        version = 536870912,
                        prevBlockhash = "00000000000000000002abc123def456abc123def456abc123def456abc123de",
                        merkleRoot = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
                        time = 1699564800,
                        bits = 386089497,
                        nonce = 2083236893
                    ),
                    blockHash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                    blockHeight = "943609"
                ),
                onAction = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewEmpty() {
    MandacaruTheme {
        Surface {
            ScreenBlockchainContent(
                uiState = BlockchainUiState(
                    blockCount = "943,609",
                    bestBlockHash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab"
                ),
                onAction = {}
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
            ScreenBlockchainContent(
                uiState = BlockchainUiState(
                    blockCount = "943,609",
                    bestBlockHash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                    validatedBlocks = 943609,
                    blockHeader = BlockHeaderResult(
                        version = 536870912,
                        prevBlockhash = "00000000000000000002abc123def456abc123def456abc123def456abc123de",
                        merkleRoot = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
                        time = 1699564800,
                        bits = 386089497,
                        nonce = 2083236893
                    ),
                    blockHash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                    blockHeight = "943609"
                ),
                onAction = {}
            )
        }
    }
}
