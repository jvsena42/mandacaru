package com.github.jvsena42.floresta_node.domain.floresta

import com.florestad.Network

fun String.toFlorestaNetwork(): Network {
    return Network.entries.firstOrNull { it.name == this } ?: Network.SIGNET
}