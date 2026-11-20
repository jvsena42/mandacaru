package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.utils.FlagGlyphSupport
import com.github.jvsena42.mandacaru.presentation.utils.countryCodeToDisplayName

/**
 * A peer's country flag. The slot has a minimum width so addresses stay column-aligned whether
 * or not a peer resolved, and grows with the glyph at large font scales. It renders nothing at
 * all for private, onion, or unknown peers — including before the GeoIP database has been
 * downloaded.
 */
@Composable
internal fun PeerFlag(countryCode: String?, flag: String?) {
    Box(
        modifier = Modifier.widthIn(min = FLAG_SLOT_WIDTH),
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

/** Takes the flag's slot: onion peers have no country, and the badge keeps them apart. */
@Composable
internal fun OnionPeerBadge() {
    val description = stringResource(R.string.peer_onion_content_description)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.widthIn(min = FLAG_SLOT_WIDTH),
    ) {
        Text(
            stringResource(R.string.peer_onion_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("node_peer_onion")
                .semantics { contentDescription = description },
        )
    }
}

private val FLAG_SLOT_WIDTH = 24.dp
