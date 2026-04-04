package com.github.jvsena42.floresta_node.presentation.utils

import com.florestad.Network
import com.github.jvsena42.floresta_node.domain.model.Constants

fun String.removeSpaces() = this.replace(" ", "")

fun String.getNetwork() : Network {
    return Network.entries.find { it.name == this } ?: Network.SIGNET
}

fun Network.getRpcPort() : String {
    return when(this) {
        Network.BITCOIN -> Constants.RPC_PORT_MAINNET
        Network.SIGNET -> Constants.RPC_PORT_SIGNET
        Network.TESTNET -> Constants.RPC_PORT_TESTNET
        Network.REGTEST -> Constants.RPC_PORT_REGTEST
        Network.TESTNET4 -> Constants.RPC_PORT_TESTNET_4
    }
}

fun Float.toScientificNotationString(): String {
    val stringValue = this.toString()
    if (!stringValue.contains('E', ignoreCase = true)) {
        return stringValue
    }

    val parts = stringValue.split('E', ignoreCase = true)
    if (parts.size != 2) {
        return stringValue // Invalid format
    }

    val coefficient = parts[0]
    val exponent = parts[1].toIntOrNull()

    if (exponent == null) {
        return stringValue // Invalid exponent
    }

    return "${coefficient}x10${'\u00B3'.takeIf { exponent == 3 } ?: exponent.toSuperscript()}"
}

private fun Int.toSuperscript(): String {
    val superscripts = mapOf(
        '0' to '\u2070',
        '1' to '\u00B9',
        '2' to '\u00B2',
        '3' to '\u00B3',
        '4' to '\u2074',
        '5' to '\u2075',
        '6' to '\u2076',
        '7' to '\u2077',
        '8' to '\u2078',
        '9' to '\u2079',
        '+' to '\u207A',
        '-' to '\u207B'
    )

    return this.toString().map { superscripts[it] ?: it }.joinToString("")
}

fun Float.toHumanReadableDifficulty(): String {
    return when {
        this >= 1e12f -> "%.2f T".format(this / 1e12f)
        this >= 1e9f -> "%.2f B".format(this / 1e9f)
        this >= 1e6f -> "%.2f M".format(this / 1e6f)
        this >= 1e3f -> "%.2f K".format(this / 1e3f)
        else -> "%.2f".format(this)
    }
}

fun Double.toScientificNotationString(): String {
    val stringValue = this.toString()
    if (!stringValue.contains('E', ignoreCase = true)) {
        return stringValue
    }

    val parts = stringValue.split('E', ignoreCase = true)
    if (parts.size != 2) {
        return stringValue // Invalid format
    }

    val coefficient = parts[0]
    val exponent = parts[1].toIntOrNull()

    if (exponent == null) {
        return stringValue // Invalid exponent
    }

    return "${coefficient}x10${'\u00B3'.takeIf { exponent == 3 } ?: exponent.toSuperscript()}"
}