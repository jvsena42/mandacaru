package com.github.jvsena42.mandacaru.domain.settings

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource

private val ALLOWED_PORTS = 1..65535
private const val IPV4_OCTETS = 4
private const val MAX_OCTET_DIGITS = 3
private const val MAX_OCTET_VALUE = 255

data class TorProxyAddress(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"
}

/** Orbot's SOCKS5 listener. */
val DEFAULT_TOR_PROXY = TorProxyAddress(host = "127.0.0.1", port = 9050)

sealed interface TorProxyInput {
    data object Default : TorProxyInput
    data class Custom(val address: TorProxyAddress) : TorProxyInput
    data object Invalid : TorProxyInput
}

/**
 * Only `IPv4:port` is accepted, which covers Orbot's loopback listener. A hostname would make
 * both the app's reachability check and the node resolve it through the system resolver.
 */
fun parseTorProxy(input: String): TorProxyInput {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return TorProxyInput.Default
    val parts = trimmed.split(':')
    if (parts.size != 2) return TorProxyInput.Invalid
    val (host, portText) = parts
    val canonicalHost = canonicalIpv4(host) ?: return TorProxyInput.Invalid
    if (portText.isEmpty() || portText.any { !it.isDigit() }) return TorProxyInput.Invalid
    val port = portText.toIntOrNull()?.takeIf { it in ALLOWED_PORTS } ?: return TorProxyInput.Invalid
    val address = TorProxyAddress(canonicalHost, port)
    return if (address == DEFAULT_TOR_PROXY) {
        TorProxyInput.Default
    } else {
        TorProxyInput.Custom(address)
    }
}

/**
 * Rewrites leading zeros away ("010.0.0.1" -> "10.0.0.1"): Rust's IPv4 parser rejects them,
 * which would push the node onto its hostname path, where a resolver may read them as octal.
 */
private fun canonicalIpv4(host: String): String? {
    val octets = host.split('.')
    if (octets.size != IPV4_OCTETS) return null
    val values = octets.map { octet ->
        if (octet.isEmpty() || octet.length > MAX_OCTET_DIGITS || octet.any { !it.isDigit() }) {
            return null
        }
        octet.toInt().takeIf { it <= MAX_OCTET_VALUE } ?: return null
    }
    return values.joinToString(".")
}

enum class TorMode {
    OFF,

    /** Onion peers go through Tor; every other peer connects directly. */
    ONION_PEERS,

    /** Every peer connection goes through Tor. */
    ALL,
}

/** How the node uses Tor: which peers go through [proxy]. */
data class TorRoute(val mode: TorMode, val proxy: TorProxyAddress) {
    init {
        require(mode != TorMode.OFF) { "TorRoute needs a Tor mode" }
    }

    /** The node's general proxy, carrying every peer connection. */
    val allPeersProxy: String?
        get() = proxy.toString().takeIf { mode == TorMode.ALL }

    /** The node's onion-only proxy, carrying only onion peers. */
    val onionPeersProxy: String?
        get() = proxy.toString().takeIf { mode == TorMode.ONION_PEERS }
}

/**
 * Before modes existed Tor was a switch, and on meant every connection over Tor. A saved mode
 * this build does not recognise is read as [TorMode.ALL], so a corrupt value can only make the
 * node more private, never silently drop Tor. Saving a mode keeps the switch in step, so a
 * saved Off with the switch on means an older build turned Tor on since: that wins.
 */
suspend fun PreferencesDataSource.getTorMode(): TorMode {
    val legacyEnabled = getBoolean(PreferenceKeys.TOR_ENABLED, false)
    val saved = getString(PreferenceKeys.TOR_MODE, "")
    if (saved.isEmpty()) return if (legacyEnabled) TorMode.ALL else TorMode.OFF
    val mode = TorMode.entries.firstOrNull { it.name == saved } ?: return TorMode.ALL
    return if (mode == TorMode.OFF && legacyEnabled) TorMode.ALL else mode
}

suspend fun PreferencesDataSource.getCustomTorProxy(): String =
    getString(PreferenceKeys.CUSTOM_TOR_PROXY, "")

/**
 * The node's Tor route, or null when Tor is off. A saved proxy that no longer parses falls back
 * to the default rather than to no proxy, so a corrupt address can never silently turn Tor off.
 */
suspend fun PreferencesDataSource.torRoute(): TorRoute? {
    val mode = getTorMode()
    if (mode == TorMode.OFF) return null
    val proxy = when (val parsed = parseTorProxy(getCustomTorProxy())) {
        is TorProxyInput.Custom -> parsed.address
        TorProxyInput.Default, TorProxyInput.Invalid -> DEFAULT_TOR_PROXY
    }
    return TorRoute(mode, proxy)
}
