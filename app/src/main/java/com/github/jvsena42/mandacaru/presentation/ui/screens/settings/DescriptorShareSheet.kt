package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme
import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.github.jvsena42.mandacaru.presentation.utils.encodeQr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptorShareSheet(
    descriptor: String,
    onCopy: (value: String, isDescriptor: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        DescriptorShareSheetContent(descriptor = descriptor, onCopy = onCopy)
    }
}

@Composable
private fun DescriptorShareSheetContent(
    descriptor: String,
    onCopy: (value: String, isDescriptor: Boolean) -> Unit,
) {
    val electrumKey = remember(descriptor) { DescriptorUtils.electrumKeyFor(descriptor) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val segmentColors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primary,
            activeContentColor = MaterialTheme.colorScheme.onPrimary,
            activeBorderColor = MaterialTheme.colorScheme.primary,
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = SEGMENT_COUNT),
                colors = segmentColors,
                icon = {},
                modifier = Modifier.testTag("tab_descriptor"),
            ) {
                Text(stringResource(R.string.tab_descriptor))
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = SEGMENT_COUNT),
                colors = segmentColors,
                icon = {},
                modifier = Modifier.testTag("tab_extended_key"),
            ) {
                Text(stringResource(R.string.tab_extended_key))
            }
        }

        when (selectedTab) {
            0 -> ShareTabContent(
                value = descriptor,
                description = stringResource(R.string.share_descriptor_wallets),
                copyTestTag = "button_copy_descriptor",
                onCopy = { onCopy(descriptor, true) },
            )

            else -> if (electrumKey != null) {
                ShareTabContent(
                    value = electrumKey,
                    description = stringResource(R.string.share_electrum_wallets),
                    copyTestTag = "button_copy_extended_key",
                    onCopy = { onCopy(electrumKey, false) },
                )
            } else {
                UnavailableNotice()
            }
        }
    }
}

@Composable
private fun ShareTabContent(
    value: String,
    description: String,
    copyTestTag: String,
    onCopy: () -> Unit,
) {
    val qr = remember(value) { encodeQr(value, size = QR_SIZE_PX) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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
        } else {
            Text(
                stringResource(R.string.utreexo_error_too_large_for_qr),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth().testTag(copyTestTag),
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.utreexo_copy))
        }
    }
}

@Composable
private fun UnavailableNotice() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.share_electrum_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SEGMENT_COUNT = 2
private const val QR_SIZE_PX = 768
private const val QR_DISPLAY_DP = 280

private const val SAMPLE_DESCRIPTOR =
    "wpkh([a5b13c0e/84h/0h/0h]xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5g" +
        "fAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG/<0;1>/*)"

@PreviewLightDark
@Composable
private fun DescriptorShareSheetPreview() {
    MandacaruTheme {
        Surface {
            DescriptorShareSheetContent(descriptor = SAMPLE_DESCRIPTOR, onCopy = { _, _ -> })
        }
    }
}
