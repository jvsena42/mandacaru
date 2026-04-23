package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.encodeQr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtreexoExportQrSheet(
    payload: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        UtreexoExportQrSheetContent(payload = payload, onDismiss = onDismiss)
    }
}

@Composable
private fun UtreexoExportQrSheetContent(
    payload: String,
    onDismiss: () -> Unit,
) {
    val qr = remember(payload) { encodeQr(payload, size = QR_SIZE_PX) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.utreexo_show_qr),
            style = MaterialTheme.typography.titleLarge,
        )

        if (qr != null) {
            Box(
                modifier = Modifier
                    .size(QR_DISPLAY_DP.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = qr,
                    contentDescription = null,
                    modifier = Modifier.size((QR_DISPLAY_DP - 16).dp),
                )
            }
            Text(
                text = payload,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                maxLines = 3,
            )
        } else {
            Text(
                stringResource(R.string.utreexo_error_too_large_for_qr),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.close))
        }
    }
}

private const val QR_SIZE_PX = 768
private const val QR_DISPLAY_DP = 280

private const val SAMPLE_PAYLOAD = """{"version":1,"network":"bitcoin","block_hash":"000000000000000000009d36aae180d04aeac872adb14e22f65c8b6647a8bf79","height":939969,"leaves":2345678901,"roots":["08daaf0c6bc41531885cfcfdeb89c34bd4d06ab4b105cf0e81bd74ab082693f5","8d4166d0303d41f7023cd35b95b24455b99b2f4a2728083bba3d172727900bed"]}"""

@PreviewLightDark
@Composable
private fun UtreexoExportQrSheetPreview() {
    MandacaruTheme {
        Surface {
            UtreexoExportQrSheetContent(payload = SAMPLE_PAYLOAD, onDismiss = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun UtreexoExportQrSheetTooLargePreview() {
    MandacaruTheme {
        Surface {
            UtreexoExportQrSheetContent(
                payload = "X".repeat(OVERSIZE_PAYLOAD_LENGTH),
                onDismiss = {},
            )
        }
    }
}

private const val OVERSIZE_PAYLOAD_LENGTH = 6000
