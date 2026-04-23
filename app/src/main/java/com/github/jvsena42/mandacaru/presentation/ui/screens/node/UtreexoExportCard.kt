package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.components.ExpandableHeader
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme

@Composable
fun UtreexoExportCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onShowQrClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpandableHeader(
                title = stringResource(R.string.share_validation),
                icon = Icons.Outlined.Backup,
                isExpanded = isExpanded,
                onToggle = onToggle,
            )
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.utreexo_export_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onShowQrClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = null)
                        Text(text = " ${stringResource(R.string.utreexo_show_qr)}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onCopyClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Text(text = " ${stringResource(R.string.utreexo_copy)}")
                        }
                        OutlinedButton(
                            onClick = onShareClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Text(text = " ${stringResource(R.string.utreexo_share)}")
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UtreexoExportCardCollapsedPreview() {
    MandacaruTheme {
        Surface {
            UtreexoExportCard(
                isExpanded = false,
                onToggle = {},
                onShowQrClick = {},
                onCopyClick = {},
                onShareClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UtreexoExportCardExpandedPreview() {
    MandacaruTheme {
        Surface {
            UtreexoExportCard(
                isExpanded = true,
                onToggle = {},
                onShowQrClick = {},
                onCopyClick = {},
                onShareClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
