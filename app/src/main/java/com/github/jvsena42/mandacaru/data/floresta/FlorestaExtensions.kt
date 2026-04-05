package com.github.jvsena42.mandacaru.data.floresta

import com.florestad.Network

fun String.toFlorestaNetwork(): Network {
    return Network.entries.firstOrNull { it.name == this } ?: Network.SIGNET
}
