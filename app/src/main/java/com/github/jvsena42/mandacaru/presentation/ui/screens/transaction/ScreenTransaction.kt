package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.window.core.layout.WindowSizeClass
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.TransactionResult
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenTransaction(
    bottomContentPadding: Dp = 0.dp,
    viewModel: TransactionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenTransactionContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        bottomContentPadding = bottomContentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTransactionContent(
    uiState: TransactionUiState,
    onAction: (TransactionAction) -> Unit,
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
                currentOnAction(TransactionAction.ClearSnackBarMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (isExpandedWidth) {
                TransactionTabletDashboard(
                    uiState = uiState,
                    onAction = onAction,
                    focusManager = focusManager,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1600.dp)
                        .padding(
                            start = horizontalPadding,
                            top = 16.dp,
                            end = horizontalPadding,
                            bottom = 16.dp + bottomContentPadding,
                        ),
                )
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxContentWidth)
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        stringResource(R.string.transactions),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                    AnimatedVisibility(visible = uiState.isSearchLoading || uiState.isBroadcasting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TransactionLookupCard(
                        uiState = uiState,
                        onAction = onAction,
                        focusManager = focusManager,
                    )

                    AnimatedVisibility(visible = uiState.searchResult != null) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            uiState.searchResult?.result?.let { tx ->
                                TransactionDetailsCard(tx)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BroadcastTransactionCard(
                        uiState = uiState,
                        onAction = onAction,
                    )

                    Spacer(modifier = Modifier.height(16.dp + bottomContentPadding))
                }
            }
        }
    }
}

@Composable
internal fun TransactionLookupCard(
    uiState: TransactionUiState,
    onAction: (TransactionAction) -> Unit,
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
                stringResource(R.string.transaction_lookup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.transactionId,
                enabled = !uiState.isSearchLoading,
                onValueChange = { newText ->
                    onAction(TransactionAction.OnSearchChanged(newText.trim()))
                },
                label = { Text(stringResource(R.string.enter_the_transaction_id)) },
                placeholder = { Text("Enter 64-character transaction ID") },
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
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "${uiState.transactionId.length}/64 characters",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    stringResource(R.string.transaction_lookup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
internal fun BroadcastTransactionCard(
    uiState: TransactionUiState,
    onAction: (TransactionAction) -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.broadcast_transaction),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.rawTxHex,
                enabled = !uiState.isBroadcasting,
                onValueChange = { newText ->
                    onAction(TransactionAction.OnRawTxChanged(newText.trim()))
                },
                label = { Text(stringResource(R.string.raw_tx_hint)) },
                placeholder = { Text(stringResource(R.string.raw_tx_placeholder)) },
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onAction(TransactionAction.OnClickBroadcast) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    stringResource(R.string.broadcast_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onAction(TransactionAction.OnClickBroadcast) },
                enabled = !uiState.isBroadcasting && uiState.rawTxHex.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.broadcast))
            }

            AnimatedVisibility(visible = uiState.broadcastResult.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                stringResource(R.string.broadcast_success),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                uiState.broadcastResult,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TransactionDetailsCard(tx: TransactionResult) {
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
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Transaction Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TxDetailRow(label = "Transaction ID", value = tx.txid ?: "N/A", isMonospace = true)

            tx.confirmations?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(
                    label = "Confirmations",
                    value = it.toString(),
                    valueColor = if (it >= 6) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            tx.blocktime?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(it * 1000))
                TxDetailRow(label = "Block Time", value = date)
            }

            tx.blockhash?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Block Hash", value = it, isMonospace = true)
            }

            tx.size?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Size", value = "$it bytes")
            }

            tx.vsize?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Virtual Size", value = "$it vBytes")
            }

            tx.weight?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Weight", value = "$it WU")
            }

            tx.version?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Version", value = it.toString())
            }

            tx.vin?.let { inputs ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Inputs", value = inputs.size.toString())
            }

            tx.vout?.let { outputs ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TxDetailRow(label = "Outputs", value = outputs.size.toString())
            }

            tx.inActiveChain?.let {
                if (it) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "This transaction is in the active chain",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TxDetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
            color = valueColor,
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
            ScreenTransactionContent(
                uiState = TransactionUiState(
                    searchResult = GetTransactionResponse(
                        id = 1,
                        jsonrpc = "2.0",
                        result = TransactionResult(
                            txid = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                            confirmations = 8,
                            blockhash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                            blocktime = 1699564800,
                            size = 250,
                            vsize = 141,
                            weight = 562,
                            version = 2,
                            inActiveChain = true,
                            hash = "abc123def456",
                            hex = null,
                            locktime = null,
                            time = null,
                            vin = listOf(),
                            vout = listOf()
                        )
                    )
                ),
                onAction = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewBroadcast() {
    MandacaruTheme {
        Surface {
            ScreenTransactionContent(
                uiState = TransactionUiState(
                    broadcastResult = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1"
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
            ScreenTransactionContent(uiState = TransactionUiState(), onAction = {})
        }
    }
}

@Preview(name = "Tablet", widthDp = 840, heightDp = 1280)
@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 840)
@Composable
private fun TabletPreview() {
    MandacaruTheme {
        Surface {
            ScreenTransactionContent(
                uiState = TransactionUiState(
                    transactionId = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                    searchResult = GetTransactionResponse(
                        id = 1,
                        jsonrpc = "2.0",
                        result = TransactionResult(
                            txid = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                            confirmations = 8,
                            blockhash = "00000000000000000001234567890abcdef1234567890abcdef1234567890ab",
                            blocktime = 1699564800,
                            size = 250,
                            vsize = 141,
                            weight = 562,
                            version = 2,
                            inActiveChain = true,
                            hash = "abc123def456",
                            hex = null,
                            locktime = null,
                            time = null,
                            vin = listOf(),
                            vout = listOf()
                        )
                    ),
                    rawTxHex = "0200000001abcdef...",
                    broadcastResult = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                ),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Tablet empty", widthDp = 840, heightDp = 1280)
@Composable
private fun TabletPreviewEmpty() {
    MandacaruTheme {
        Surface {
            ScreenTransactionContent(uiState = TransactionUiState(), onAction = {})
        }
    }
}
