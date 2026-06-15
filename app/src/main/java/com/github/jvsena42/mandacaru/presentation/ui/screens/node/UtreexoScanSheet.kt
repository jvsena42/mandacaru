package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.components.QrCameraPreview
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.RequestCameraPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtreexoScanSheet(
    onPayloadScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    onPasteFallback: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasCamera by remember { mutableStateOf(false) }
    var alreadyFired by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.utreexo_scan_qr),
                style = MaterialTheme.typography.titleLarge,
            )

            RequestCameraPermission(onPermissionChange = { hasCamera = it })

            if (hasCamera) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    QrCameraPreview(
                        enabled = !alreadyFired,
                        onPayloadScanned = {
                            if (!alreadyFired) {
                                alreadyFired = true
                                onPayloadScanned(it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            } else {
                CameraDeniedFallback(onPasteFallback = onPasteFallback)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onPasteFallback, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                Text(text = " ${stringResource(R.string.utreexo_paste_payload)}")
            }
        }
    }
}

@Composable
private fun CameraDeniedFallback(onPasteFallback: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.utreexo_scan_camera_denied),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onPasteFallback) {
            Text(stringResource(R.string.utreexo_paste_payload))
        }
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

private const val SHEET_HEIGHT_FRACTION = 0.9f

@PreviewLightDark
@Composable
private fun UtreexoScanSheetPreview() {
    MandacaruTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.utreexo_scan_qr),
                    style = MaterialTheme.typography.titleLarge,
                )
                CameraDeniedFallback(onPasteFallback = {})
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                    Text(text = " ${stringResource(R.string.utreexo_paste_payload)}")
                }
            }
        }
    }
}
