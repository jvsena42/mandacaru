package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.components.QrCameraPreview
import com.github.jvsena42.mandacaru.presentation.utils.RequestCameraPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScanSheet(
    uiState: TransactionUiState,
    onAction: (TransactionAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasCamera by remember { mutableStateOf(false) }
    var pasteMode by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { onAction(TransactionAction.OnDismissScanner) },
        sheetState = sheetState,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight(SHEET_HEIGHT_FRACTION)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.scan_transaction_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.scan_transaction_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                ScanStatus(uiState = uiState)

                if (pasteMode) {
                    PasteSection(
                        text = pasteText,
                        onTextChange = { pasteText = it },
                        enabled = !uiState.isDecoding,
                        onSubmit = { onAction(TransactionAction.OnScanPasteSubmitted(pasteText.trim())) },
                        onBackToCamera = { pasteMode = false },
                    )
                } else {
                    RequestCameraPermission(onPermissionChange = { hasCamera = it })
                    if (hasCamera) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            QrCameraPreview(
                                enabled = uiState.scanError.isEmpty() && !uiState.isDecoding,
                                onPayloadScanned = { onAction(TransactionAction.OnQrFrameScanned(it)) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    } else {
                        CameraDeniedFallback()
                    }
                    OutlinedButton(
                        onClick = { pasteMode = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                        Text(text = " ${stringResource(R.string.scan_paste)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanStatus(uiState: TransactionUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            uiState.isDecoding -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.scan_decoding), style = MaterialTheme.typography.bodyMedium)
            }

            uiState.scanProgress > 0f -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearProgressIndicator(
                    progress = { uiState.scanProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.scan_progress, (uiState.scanProgress * PERCENT).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (uiState.scanError.isNotEmpty()) {
            Text(
                uiState.scanError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PasteSection(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
    onBackToCamera: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.scan_paste_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 4,
            maxLines = 10,
            placeholder = { Text(stringResource(R.string.scan_paste_placeholder)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onBackToCamera, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_back_to_camera))
            }
            Button(
                onClick = onSubmit,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.scan_submit))
            }
        }
    }
}

@Composable
private fun CameraDeniedFallback() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.utreexo_scan_camera_denied),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
        ) {
            Text(stringResource(R.string.open_app_settings))
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.92f
private const val PERCENT = 100
private val MAX_CONTENT_WIDTH = 520.dp
