package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.florestad.Network
import com.github.jvsena42.mandacaru.BuildConfig
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.Constants
import com.github.jvsena42.mandacaru.presentation.ui.components.ExpandableHeader
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScreenSettings(
    restartApplication: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentRestartApplication by rememberUpdatedState(restartApplication)
    val context = LocalContext.current
    ScreenSettings(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvents.OnNetworkChanged -> currentRestartApplication()
                is SettingsEvents.OnExportLogs -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_logs)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenSettings(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit, modifier: Modifier = Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(uiState.snackBarMessage) {
        if (uiState.snackBarMessage.isNotEmpty()) {
            scope.launch {
                snackBarHostState.showSnackbar(message = uiState.snackBarMessage)
                currentOnAction(SettingsAction.ClearSnackBarMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { contentPadding ->
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
        ) {
            item {
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            item {
                AnimatedVisibility(visible = uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }

            // Electrum Address Card
            item {
                val clipboardManager = LocalClipboardManager.current
                val message = stringResource(R.string.node_address_copied_to_clipboard)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(uiState.electrumAddress))
                            scope.launch {
                                snackBarHostState.showSnackbar(message = message)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Node Address",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                uiState.electrumAddress,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Descriptors Section
            item {
                SectionCard(
                    title = stringResource(R.string.descriptors),
                    icon = Icons.Outlined.Wallet,
                    isExpanded = uiState.isDescriptorsExpanded,
                    onToggle = { onAction(SettingsAction.ToggleDescriptorsExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.descriptors.isNotEmpty()) {
                            uiState.descriptors.forEach { descriptor ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = descriptor,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Text(
                                text = stringResource(R.string.no_descriptors_loaded),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = uiState.descriptorText,
                            enabled = !uiState.isLoading,
                            onValueChange = { newText ->
                                onAction(SettingsAction.OnDescriptorChanged(newText))
                            },
                            label = { Text(stringResource(R.string.set_your_wallet_descriptor)) },
                            placeholder = { Text(stringResource(R.string.descriptor_placeholder)) },
                            maxLines = 1,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onAction(SettingsAction.OnClickUpdateDescriptor) }
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
                                stringResource(R.string.descriptor_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onAction(SettingsAction.OnClickUpdateDescriptor) },
                            enabled = !uiState.isLoading && uiState.descriptorText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.update_descriptor))
                        }
                    }
                }
            }

            // Network Section
            item {
                SectionCard(
                    title = stringResource(R.string.network),
                    icon = Icons.Outlined.NetworkCheck,
                    isExpanded = uiState.isNetworkExpanded,
                    onToggle = { onAction(SettingsAction.ToggleNetworkExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedNetwork,
                                readOnly = true,
                                onValueChange = { },
                                label = { Text(stringResource(R.string.select_a_network)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.network.forEach { network ->
                                    DropdownMenuItem(
                                        text = { Text(network.name) },
                                        onClick = {
                                            onAction(SettingsAction.OnNetworkSelected(network.name))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                stringResource(R.string.the_application_will_be_restarted_to_update_the_network),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Fast Sync Section
            item {
                val uriHandler = LocalUriHandler.current

                SectionCard(
                    title = stringResource(R.string.fast_sync),
                    icon = Icons.Outlined.Speed,
                    isExpanded = uiState.isFastSyncExpanded,
                    onToggle = { onAction(SettingsAction.ToggleFastSyncExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.fast_sync_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.isFastSyncEnabled,
                                onCheckedChange = { onAction(SettingsAction.OnFastSyncToggled(it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            stringResource(R.string.fast_sync_learn_more),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://bitcoinops.org/en/topics/assumeutxo/")
                            }
                        )
                    }
                }
            }

            // Node Section
            item {
                SectionCard(
                    title = stringResource(R.string.node),
                    icon = Icons.Outlined.Refresh,
                    isExpanded = uiState.isNodeExpanded,
                    onToggle = { onAction(SettingsAction.ToggleNodeExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.nodeAddress,
                            enabled = !uiState.isLoading,
                            onValueChange = { newText ->
                                onAction(SettingsAction.OnNodeAddressChanged(newText))
                            },
                            label = { Text(stringResource(R.string.connect_directly_with_a_node)) },
                            placeholder = { Text(stringResource(R.string.node_address_placeholder)) },
                            maxLines = 1,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onAction(SettingsAction.OnClickConnectNode) }
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
                                stringResource(R.string.node_address_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onAction(SettingsAction.OnClickConnectNode) },
                            enabled = !uiState.isLoading && uiState.nodeAddress.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.connect))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = { onAction(SettingsAction.OnClickRescan) },
                            enabled = !uiState.isLoading && uiState.descriptors.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.rescan))
                        }
                    }
                }
            }

            // Export Logs
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.logs),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        FilledTonalButton(
                            onClick = { onAction(SettingsAction.OnClickExportLogs) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.export_logs))
                        }
                    }
                }
            }

            // Donate Section
            item {
                val clipboardManager = LocalClipboardManager.current
                val donateAddress = "jvsena42@blink.sv"
                val copiedMessage = stringResource(R.string.lightning_address_copied)

                SectionCard(
                    title = stringResource(R.string.donate),
                    icon = Icons.Outlined.Favorite,
                    isExpanded = uiState.isDonateExpanded,
                    onToggle = { onAction(SettingsAction.ToggleDonateExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.lightning_address),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(donateAddress))
                                    scope.launch {
                                        snackBarHostState.showSnackbar(message = copiedMessage)
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = donateAddress,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // About Section
            item {
                val uriHandler = LocalUriHandler.current

                SectionCard(
                    title = stringResource(R.string.about),
                    icon = Icons.Outlined.Info,
                    isExpanded = uiState.isAboutExpanded,
                    onToggle = { onAction(SettingsAction.ToggleAboutExpanded) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${stringResource(R.string.app_name)} — ${stringResource(R.string.version, BuildConfig.VERSION_NAME)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.suggestions_and_bugs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/jvsena42/mandacaru/issues")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.license),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/jvsena42/mandacaru/blob/main/LICENSE")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.check_for_updates),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/jvsena42/mandacaru/releases/latest")
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpandableHeader(
                title = title,
                icon = icon,
                isExpanded = isExpanded,
                onToggle = onToggle
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    MandacaruTheme {
        Surface {
            ScreenSettings(
                uiState = SettingsUiState(
                    electrumAddress = Constants.ELECTRUM_ADDRESS,
                    selectedNetwork = Network.SIGNET.name,
                    descriptors = listOf(
                        "wpkh([d34db33f/84'/0'/0']xpub6CUGRUo...)",
                        "wpkh([d34db33f/84'/0'/1']xpub6CUGRUo...)"
                    )
                ),
                onAction = {}
            )
        }
    }
}
