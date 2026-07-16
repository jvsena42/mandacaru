package com.github.jvsena42.mandacaru.presentation.utils

import android.graphics.Paint

/**
 * Whether the system font can actually draw flag emoji. Most ROMs can; some render the two
 * regional indicator letters instead, in which case we show the country code deliberately
 * rather than letting a half-broken glyph pair through.
 *
 * The answer is a property of the device font, so it is resolved once.
 */
object FlagGlyphSupport {

    val isSupported: Boolean by lazy {
        val probe = countryCodeToFlagEmoji(PROBE_COUNTRY) ?: return@lazy false
        runCatching { Paint().hasGlyph(probe) }.getOrDefault(false)
    }

    private const val PROBE_COUNTRY = "BR"
}
