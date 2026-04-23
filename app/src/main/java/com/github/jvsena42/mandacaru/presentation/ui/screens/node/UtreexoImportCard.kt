package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.QrCodeScanner
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
fun UtreexoImportCard(
    onScanClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    stringResource(R.string.utreexo_import_cta_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                stringResource(R.string.utreexo_import_cta_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                    )
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = " ${stringResource(R.string.utreexo_scan_qr)}",
                    )
                }
                OutlinedButton(
                    onClick = onPasteClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                    )
                    Text(
                        text = " ${stringResource(R.string.utreexo_paste_payload)}",
                    )
                }
            }
        }
    }
}
