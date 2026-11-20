package com.github.jvsena42.mandacaru.domain.settings

import com.florestad.Network
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.presentation.utils.getElectrumPort
import com.github.jvsena42.mandacaru.presentation.utils.getNetwork
import com.github.jvsena42.mandacaru.presentation.utils.getRpcPort

const val ELECTRUM_HOST = "127.0.0.1"

// Electrum's server notation: `t` marks plaintext TCP, `s` would be SSL.
private const val ELECTRUM_TCP_SUFFIX = "t"

fun electrumServerAddress(port: String): String = "$ELECTRUM_HOST:$port:$ELECTRUM_TCP_SUFFIX"

// Android apps cannot bind privileged ports.
private val ALLOWED_PORTS = 1024..65535

sealed interface ElectrumPortInput {
    data object Default : ElectrumPortInput
    data class Custom(val port: Int) : ElectrumPortInput
    data object OutOfRange : ElectrumPortInput
    data object ConflictsWithRpc : ElectrumPortInput
}

fun parseElectrumPort(input: String, network: Network): ElectrumPortInput {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ElectrumPortInput.Default
    val port = trimmed.toIntOrNull()
    return when {
        port == null || port !in ALLOWED_PORTS -> ElectrumPortInput.OutOfRange
        port.toString() == network.getRpcPort() -> ElectrumPortInput.ConflictsWithRpc
        port.toString() == network.getElectrumPort() -> ElectrumPortInput.Default
        else -> ElectrumPortInput.Custom(port)
    }
}

suspend fun PreferencesDataSource.getSelectedNetwork(): Network =
    getString(PreferenceKeys.CURRENT_NETWORK, Network.BITCOIN.name).getNetwork()

suspend fun PreferencesDataSource.getCustomElectrumPort(): String =
    getString(PreferenceKeys.CUSTOM_ELECTRUM_PORT, "")

/**
 * The port the node's Electrum server listens on: the user's custom port when it is still
 * usable on [network] (it can collide with the RPC port after a network switch), otherwise
 * the network default.
 */
suspend fun PreferencesDataSource.electrumPortFor(network: Network): String =
    when (val parsed = parseElectrumPort(getCustomElectrumPort(), network)) {
        is ElectrumPortInput.Custom -> parsed.port.toString()
        else -> network.getElectrumPort()
    }
