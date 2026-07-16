package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.presentation.utils.FlagGlyphSupport
import com.github.jvsena42.mandacaru.presentation.utils.countryCodeToDisplayName

/**
 * A peer's country flag. The slot keeps a fixed width so addresses stay column-aligned whether
 * or not a peer resolved, and renders nothing at all for private, onion, or unknown peers —
 * including before the GeoIP database has been downloaded.
 */
@Composable
internal fun PeerFlag(countryCode: String?, flag: String?) {
    Box(
        modifier = Modifier.width(FLAG_SLOT_WIDTH),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (countryCode == null) return@Box
        val countryName = remember(countryCode) { countryCodeToDisplayName(countryCode) }

        if (flag != null && FlagGlyphSupport.isSupported) {
            Text(
                flag,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .testTag("node_peer_flag")
                    .semantics { contentDescription = countryName },
            )
        } else {
            // Devices whose system font lacks flag glyphs show the country code instead.
            Text(
                countryCode,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .testTag("node_peer_flag")
                    .semantics { contentDescription = countryName },
            )
        }
    }
}

private val FLAG_SLOT_WIDTH = 24.dp
