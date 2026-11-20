package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.github.jvsena42.mandacaru.R

@Composable
internal fun TorProxyUnreachableCard(proxy: String) {
    ErrorWarningCard(
        title = stringResource(R.string.tor_proxy_unreachable_title),
        message = stringResource(R.string.tor_proxy_unreachable_message, proxy),
        titleModifier = Modifier.testTag("node_tor_unreachable"),
    )
}

@Composable
internal fun TorRestartRequiredCard(onRestart: () -> Unit) {
    ErrorWarningCard(
        title = stringResource(R.string.tor_restart_required_title),
        message = stringResource(R.string.tor_restart_required_message),
        titleModifier = Modifier.testTag("node_tor_restart_required"),
    ) {
        TextButton(
            onClick = onRestart,
            modifier = Modifier.testTag("button_restart_for_tor"),
        ) {
            Text(
                stringResource(R.string.restart_now),
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
