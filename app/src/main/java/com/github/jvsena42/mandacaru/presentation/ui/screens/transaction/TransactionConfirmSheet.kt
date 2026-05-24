package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.scan.DecodedTransaction
import com.github.jvsena42.mandacaru.domain.scan.PayloadType
import com.github.jvsena42.mandacaru.domain.scan.ScanTransport
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionConfirmSheet(
    decoded: DecodedTransaction,
    isBroadcasting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        TransactionConfirmContent(
            decoded = decoded,
            isBroadcasting = isBroadcasting,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TransactionConfirmContent(
    decoded: DecodedTransaction,
    isBroadcasting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.confirm_broadcast_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    R.string.confirm_decoded_from,
                    stringResource(sourceLabel(decoded.payloadType)),
                    stringResource(transportLabel(decoded.transport)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SummaryCard(decoded)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isBroadcasting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isBroadcasting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.broadcast))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(decoded: DecodedTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(
                label = stringResource(R.string.confirm_tx_id),
                value = decoded.txid,
                isMonospace = true,
            )
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow(
                    label = stringResource(R.string.confirm_inputs),
                    value = decoded.inputCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                DetailRow(
                    label = stringResource(R.string.confirm_outputs),
                    value = decoded.outputCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Divider()
            DetailRow(
                label = stringResource(R.string.confirm_total_out),
                value = stringResource(R.string.value_sats, groupSats(decoded.totalOutSats)),
            )
            Divider()
            FeeRow(decoded)
            decoded.feeRateSatVb?.let { rate ->
                Divider()
                DetailRow(
                    label = stringResource(R.string.confirm_fee_rate),
                    value = stringResource(R.string.value_sat_vb, formatFeeRate(rate)),
                )
            }
            Divider()
            DetailRow(
                label = stringResource(R.string.confirm_virtual_size),
                value = stringResource(R.string.value_vbytes, groupSats(decoded.vsize)),
            )
        }
    }
}

@Composable
private fun FeeRow(decoded: DecodedTransaction) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (decoded.feeSats != null) {
            DetailRow(
                label = stringResource(R.string.confirm_fee),
                value = stringResource(R.string.value_sats, groupSats(decoded.feeSats)),
            )
        } else {
            DetailRow(
                label = stringResource(R.string.confirm_fee),
                value = stringResource(R.string.confirm_fee_unavailable),
            )
            Text(
                stringResource(R.string.confirm_fee_unavailable_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = LABEL_ALPHA),
            fontWeight = FontWeight.Medium,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = VALUE_BG_ALPHA))
                .padding(8.dp),
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
}

private fun sourceLabel(type: PayloadType): Int = when (type) {
    PayloadType.PSBT -> R.string.confirm_source_psbt
    PayloadType.TRANSACTION -> R.string.confirm_source_transaction
}

private fun transportLabel(transport: ScanTransport): Int = when (transport) {
    ScanTransport.SINGLE -> R.string.confirm_transport_single
    ScanTransport.UR -> R.string.confirm_transport_ur
    ScanTransport.BBQR -> R.string.confirm_transport_bbqr
}

private fun groupSats(value: Long): String = String.format(Locale.getDefault(), "%,d", value)

private fun formatFeeRate(rate: Double): String = String.format(Locale.getDefault(), "%.1f", rate)

private const val LABEL_ALPHA = 0.7f
private const val VALUE_BG_ALPHA = 0.3f
private val MAX_CONTENT_WIDTH = 560.dp

@PreviewLightDark
@Composable
private fun TransactionConfirmContentPreview() {
    MandacaruTheme {
        Surface {
            TransactionConfirmContent(
                decoded = DecodedTransaction(
                    rawHex = "0200000001abcdef…",
                    txid = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                    inputCount = 1,
                    outputCount = 2,
                    totalOutSats = 1_234_567,
                    feeSats = 2_500,
                    feeRateSatVb = 12.3,
                    vsize = 141,
                    weight = 562,
                    payloadType = PayloadType.PSBT,
                    transport = ScanTransport.UR,
                ),
                isBroadcasting = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransactionConfirmContentRawTxPreview() {
    MandacaruTheme {
        Surface {
            TransactionConfirmContent(
                decoded = DecodedTransaction(
                    rawHex = "0200000001abcdef…",
                    txid = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                    inputCount = 2,
                    outputCount = 1,
                    totalOutSats = 980_000,
                    feeSats = null,
                    feeRateSatVb = null,
                    vsize = 222,
                    weight = 888,
                    payloadType = PayloadType.TRANSACTION,
                    transport = ScanTransport.BBQR,
                ),
                isBroadcasting = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
