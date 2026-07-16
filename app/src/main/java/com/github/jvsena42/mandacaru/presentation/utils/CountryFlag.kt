package com.github.jvsena42.mandacaru.presentation.utils

import java.util.Locale

private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val COUNTRY_CODE_LENGTH = 2

/** Two regional indicators, each a surrogate pair. */
private const val FLAG_LENGTH = 4

/**
 * Maps an ISO 3166-1 alpha-2 code to its flag emoji — the pair of regional indicator symbols
 * that renders as a flag. Returns null for anything that is not two ASCII letters, which
 * covers the database's "unknown" markers.
 */
fun countryCodeToFlagEmoji(countryCode: String): String? {
    if (countryCode.length != COUNTRY_CODE_LENGTH) return null
    val first = countryCode[0].uppercaseChar()
    val second = countryCode[1].uppercaseChar()
    if (first !in 'A'..'Z' || second !in 'A'..'Z') return null
    return StringBuilder(FLAG_LENGTH)
        .appendCodePoint(REGIONAL_INDICATOR_A + (first - 'A'))
        .appendCodePoint(REGIONAL_INDICATOR_A + (second - 'A'))
        .toString()
}

/**
 * Localized country name for screen readers, e.g. "Brazil"/"Brasil". Falls back to the code
 * itself when the platform has no name for it.
 */
fun countryCodeToDisplayName(countryCode: String): String =
    Locale("", countryCode).displayCountry.ifEmpty { countryCode }
