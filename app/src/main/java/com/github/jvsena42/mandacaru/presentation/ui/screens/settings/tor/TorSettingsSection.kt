package com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.SectionCard

@Composable
internal fun TorSettingsSection(
    state: TorSettingsUiState,
    onAction: (TorSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.tor),
        icon = Icons.Outlined.VpnLock,
        isExpanded = state.isExpanded,
        onToggle = { onAction(TorSettingsAction.ToggleExpanded) },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.connect_over_tor),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.connect_over_tor_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isTorEnabled,
                    enabled = state.isLoaded && !state.isApplying,
                    onCheckedChange = { onAction(TorSettingsAction.OnToggleTor(it)) },
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .testTag("toggle_tor"),
                )
            }

            AnimatedVisibility(visible = state.isTorEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.proxyText,
                        enabled = !state.isApplying,
                        onValueChange = { onAction(TorSettingsAction.OnProxyChanged(it)) },
                        label = { Text(stringResource(R.string.tor_proxy_address)) },
                        placeholder = { Text(state.defaultProxy) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = state.proxyError != null,
                        supportingText = {
                            Text(
                                state.proxyError?.let { stringResource(it) }
                                    ?: stringResource(
                                        R.string.tor_proxy_supporting_text,
                                        state.defaultProxy,
                                    )
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onAction(TorSettingsAction.OnClickApply) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tor_proxy"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onAction(TorSettingsAction.OnClickApply) },
                enabled = state.isLoaded && !state.isApplying && state.hasChanges,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_apply_tor"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.apply_and_restart))
            }
        }
    }
}
