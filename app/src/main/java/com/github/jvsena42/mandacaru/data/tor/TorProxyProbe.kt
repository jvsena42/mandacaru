package com.github.jvsena42.mandacaru.data.tor

import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.domain.settings.TorProxyAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

fun interface TorProxyProbe {
    suspend fun isReachable(proxy: TorProxyAddress): Boolean
}

/**
 * Opens a connection to the proxy and runs the SOCKS5 greeting (RFC 1928 §3) offering only
 * "no authentication", which is what Tor accepts. A plain open port is not enough: anything
 * listening there that is not a SOCKS5 server would leave the node failing every connection.
 * The connection is closed right after the method reply, before any CONNECT is sent.
 */
class Socks5TorProxyProbe(
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TorProxyProbe {

    override suspend fun isReachable(proxy: TorProxyAddress): Boolean = withContext(ioDispatcher) {
        runSuspendCatching { greet(proxy) }
            .onFailure { if (it !is IOException && it !is SecurityException) throw it }
            .getOrDefault(false)
    }

    private fun greet(proxy: TorProxyAddress): Boolean = Socket().use { socket ->
        // The host is an IPv4 literal, so this never reaches a resolver.
        val address = InetSocketAddress(InetAddress.getByName(proxy.host), proxy.port)
        socket.connect(address, timeoutMs)
        socket.soTimeout = timeoutMs
        socket.getOutputStream().apply {
            write(byteArrayOf(SOCKS_VERSION, ONE_METHOD, NO_AUTH))
            flush()
        }
        val input = socket.getInputStream()
        val version = input.read()
        val method = input.read()
        version == SOCKS_VERSION.toInt() && method == NO_AUTH.toInt()
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000
        const val SOCKS_VERSION: Byte = 0x05
        const val ONE_METHOD: Byte = 0x01
        const val NO_AUTH: Byte = 0x00
    }
}
