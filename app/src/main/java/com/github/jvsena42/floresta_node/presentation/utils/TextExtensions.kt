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
    }
}