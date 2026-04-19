package com.github.jvsena42.mandacaru.domain.floresta

// NODE_UTREEXO service flag: bit 12 (see Floresta/crates/floresta-common/src/lib.rs).
// rust-bitcoin's ServiceFlags::Display renders unknown-to-rust-bitcoin bits in hex,
// so a peer with the Utreexo flag shows up inside ServiceFlags(...) as the token "0x1000".
private const val UTREEXO_BIT = 0x1000L
private const val HEX_RADIX = 16

fun String.hasUtreexoServiceFlag(): Boolean {
    val inside = substringAfter("ServiceFlags(", "").substringBefore(")")
    if (inside.isEmpty()) return false
    return inside.split("|").any { token ->
        val t = token.trim()
        if (!t.startsWith("0x", ignoreCase = true)) return@any false
        val n = t.drop(2).toLongOrNull(HEX_RADIX) ?: return@any false
        (n and UTREEXO_BIT) != 0L
    }
}
