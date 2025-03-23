package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.florestad.Network
import com.github.jvsena42.floresta_node.R
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScreenSettings(
    viewModel: SettingsViewModel = koinViewModel(),
    restartApplication: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenSettings(uiState = uiState, onAction = viewModel::onAction)
    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvents.OnNetworkChanged -> restartApplication()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenSettings(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            scope.launch {
                snackBarHostState.showSnackbar(message = uiState.errorMessage)
                onAction(SettingsAction.ClearSnackBarMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(32.dp))

            val clipboardManager = LocalClipboardManager.current
            val message = stringResource(R.string.node_address_copied_to_clipboard)

            Text(
                text = stringResource(R.string.node_address, uiState.signetAddress),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(uiState.signetAddress))
                        scope.launch {
                            snackBarHostState.showSnackbar(message = message)
                            onAction(SettingsAction.ClearSnackBarMessage)
                        }
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = uiState.descriptorText,
                enabled = !uiState.isLoading,
                onValueChange = { newText -> onAction(SettingsAction.OnDescriptorChanged(newText)) },
                label = { Text(stringResource(R.string.set_your_wallet_descriptor)) },
                placeholder = { Text(stringResource(R.string.descriptor_placeholder)) },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(SettingsAction.OnClickUpdateDescriptor) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onAction(SettingsAction.OnClickUpdateDescriptor) },
                enabled = !uiState.isLoading && uiState.descriptorText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.update_descriptor))
            }

            Spacer(modifier = Modifier.height(32.dp))

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = uiState.selectedNetwork,
                    readOnly = true,
                    onValueChange = { newText -> onAction(SettingsAction.OnNetworkSelected(newText)) },
                    label = { Text(stringResource(R.string.select_a_network)) },
                    supportingText = { Text(stringResource(R.string.the_application_will_be_restarted_to_update_the_network)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
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

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = uiState.nodeAddress,
                enabled = !uiState.isLoading,
                onValueChange = { newText -> onAction(SettingsAction.OnNodeAddressChanged(newText)) },
                label = { Text(stringResource(R.string.connect_directly_with_a_node)) },
                placeholder = { Text(stringResource(R.string.node_address_placeholder)) },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(SettingsAction.OnClickConnectNode) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onAction(SettingsAction.OnClickConnectNode) },
                enabled = !uiState.isLoading && uiState.nodeAddress.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.connect))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { onAction(SettingsAction.OnClickRescan) },
                enabled = !uiState.isLoading && uiState.descriptorText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.rescan))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    FlorestaNodeTheme {
        ScreenSettings(
            uiState = SettingsUiState(
                signetAddress = Constants.ELECTRUM_ADDRESS,
                selectedNetwork = Network.SIGNET.name
            ),
            onAction = {}
        )
    }
}