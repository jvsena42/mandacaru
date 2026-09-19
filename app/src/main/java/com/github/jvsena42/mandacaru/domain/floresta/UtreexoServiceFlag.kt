package com.github.jvsena42.mandacaru.domain.floresta

// NODE_UTREEXO service flag: bit 12 (see Floresta/crates/floresta-common/src/lib.rs).
// getpeerinfo reports `services` as a Bitcoin Core-style hex bitmask, e.g. "0000000000001c49".
private const val UTREEXO_BIT = 0x1000UL
private const val HEX_RADIX = 16

fun String.hasUtreexoServiceFlag(): Boolean {
    val flags = toULongOrNull(HEX_RADIX) ?: return false
    return (flags and UTREEXO_BIT) != 0UL
}
