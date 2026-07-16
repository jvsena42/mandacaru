package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.runtime.Immutable
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import com.github.jvsena42.mandacaru.presentation.utils.countryCodeToFlagEmoji

/**
 * A peer plus its resolved country. Resolution happens once per address in the ViewModel
 * rather than in the row, because the peer list is repolled every 10 seconds.
 *
 * [countryCode] and [flag] are null for private, onion, unknown, or not-yet-resolvable
 * addresses — the row then renders exactly as it did before flags existed.
 */
@Immutable
data class PeerUi(
    val peer: PeerInfoResult,
    val countryCode: String? = null,
    val flag: String? = null,
)

/** Pairs a peer with a country, deriving the flag. Used by the node screen previews. */
internal fun PeerInfoResult.withCountry(countryCode: String) =
    PeerUi(this, countryCode, countryCodeToFlagEmoji(countryCode))
