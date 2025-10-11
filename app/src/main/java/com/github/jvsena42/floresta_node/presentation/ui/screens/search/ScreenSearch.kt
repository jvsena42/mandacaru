package com.github.jvsena42.floresta_node.presentation.ui.screens.search

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import com.github.jvsena42.floresta_node.R
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.TransactionResult
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenSearch(
    viewModel: SearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenSearchContent(uiState = uiState, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenSearchContent(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            scope.launch {
                snackBarHostState.showSnackbar(message = uiState.errorMessage)
                onAction(SearchAction.ClearSnackBarMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.search),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(visible = uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        "Transaction Lookup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.transactionId,
                        enabled = !uiState.isLoading,
                        onValueChange = { newText ->
                            onAction(SearchAction.OnSearchChanged(newText.trim()))
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
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                "${uiState.transactionId.length}/64 characters",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Results will appear automatically as you type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transaction Result
            AnimatedVisibility(visible = uiState.searchResult != null) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    uiState.searchResult?.result?.let { tx ->
                        TransactionDetailsCard(tx)
                    }
                }
            }

            // Empty State
            AnimatedVisibility(
                visible = uiState.searchResult == null
                        && !uiState.isLoading
                        && uiState.transactionId.isEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Search for Bitcoin Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter a transaction ID to view its details on the blockchain using your local node",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailsCard(tx: TransactionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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

            // Transaction ID
            TransactionDetailRow(
                label = "Transaction ID",
                value = tx.txid ?: "N/A",
                isMonospace = true
            )

            tx.confirmations?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Confirmations",
                    value = it.toString(),
                    valueColor = if (it >= 6) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }

            tx.blocktime?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(it * 1000))
                TransactionDetailRow(
                    label = "Block Time",
                    value = date
                )
            }

            tx.blockhash?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Block Hash",
                    value = it,
                    isMonospace = true
                )
            }

            tx.size?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Size",
                    value = "$it bytes"
                )
            }

            tx.vsize?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Virtual Size",
                    value = "$it vBytes"
                )
            }

            tx.weight?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Weight",
                    value = "$it WU"
                )
            }

            tx.version?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Version",
                    value = it.toString()
                )
            }

            tx.vin?.let { inputs ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Inputs",
                    value = inputs.size.toString()
                )
            }

            tx.vout?.let { outputs ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                TransactionDetailRow(
                    label = "Outputs",
                    value = outputs.size.toString()
                )
            }

            // In Active Chain indicator
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
private fun TransactionDetailRow(
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
    FlorestaNodeTheme {
        Surface {
            ScreenSearchContent(
                SearchUiState(
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
                )
            ) {}
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewEmpty() {
    FlorestaNodeTheme {
        Surface {
            ScreenSearchContent(SearchUiState()) {}
        }
    }
}