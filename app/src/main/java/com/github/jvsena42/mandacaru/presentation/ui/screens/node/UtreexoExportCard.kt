package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R

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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.utreexo_snapshot),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.utreexo_export_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onShowQrClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.QrCode2, contentDescription = null)
                            Text(text = " ${stringResource(R.string.utreexo_show_qr)}")
                        }
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
