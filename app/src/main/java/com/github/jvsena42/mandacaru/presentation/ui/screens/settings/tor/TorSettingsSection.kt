package com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.settings.TorMode
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
            Text(
                text = stringResource(R.string.tor_section_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.selectableGroup()) {
                TorModeOption(
                    mode = TorMode.OFF,
                    title = R.string.tor_mode_off,
                    subtitle = R.string.tor_mode_off_subtitle,
                    testTag = "radio_tor_off",
                    state = state,
                    onAction = onAction,
                )
                TorModeOption(
                    mode = TorMode.ONION_PEERS,
                    title = R.string.tor_mode_onion_peers,
                    subtitle = R.string.tor_mode_onion_peers_subtitle,
                    testTag = "radio_tor_onion_peers",
                    state = state,
                    onAction = onAction,
                )
                TorModeOption(
                    mode = TorMode.ALL,
                    title = R.string.tor_mode_all,
                    subtitle = R.string.tor_mode_all_subtitle,
                    testTag = "radio_tor_all",
                    state = state,
                    onAction = onAction,
                )
            }

            AnimatedVisibility(visible = state.usesTor) {
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

@Composable
private fun TorModeOption(
    mode: TorMode,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    testTag: String,
    state: TorSettingsUiState,
    onAction: (TorSettingsAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = state.mode == mode,
                enabled = state.isLoaded && !state.isApplying,
                role = Role.RadioButton,
                onClick = { onAction(TorSettingsAction.OnModeSelected(mode)) },
            )
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = state.mode == mode,
            onClick = null,
            enabled = state.isLoaded && !state.isApplying,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
