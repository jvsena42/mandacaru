package com.github.jvsena42.mandacaru.domain.geoip

import java.net.InetAddress

/**
 * Extracts the IP literal from a peer address as the Floresta daemon prints it — a Rust
 * `SocketAddr`, i.e. `1.2.3.4:8333` or `[2001:db8::1]:8333`. The port is discarded.
 *
 * Resolution is deliberately done with [InetAddress.getByAddress] over raw bytes and never
 * with `getByName`: a hostname reaching the latter would trigger a DNS lookup and leak the
 * peer address to a resolver. Anything that is not an IP literal — hostnames, .onion, junk —
 * returns null.
 */
object PeerAddressParser {

    fun parse(raw: String): InetAddress? {
        val host = extractHost(raw.trim()) ?: return null
        val bytes = parseIpv4(host) ?: parseIpv6(host) ?: return null
        return runCatching { InetAddress.getByAddress(bytes) }.getOrNull()
    }

    private fun extractHost(input: String): String? {
        if (input.isEmpty()) return null

        if (input.startsWith("[")) {
            val close = input.indexOf(']')
            if (close <= 1) return null
            return input.substring(1, close)
        }

        val colonCount = input.count { it == ':' }
        return when {
            // Unbracketed IPv6: the whole string is the host, no port can be present.
            colonCount > 1 -> input
            colonCount == 1 -> input.substringBefore(':').ifEmpty { null }
            else -> input
        }
    }

    private fun parseIpv4(host: String): ByteArray? {
        val octets = host.split('.')
        if (octets.size != IPV4_OCTETS) return null
        val bytes = ByteArray(IPV4_OCTETS)
        for ((index, octet) in octets.withIndex()) {
            if (octet.isEmpty() || octet.length > MAX_OCTET_DIGITS) return null
            if (octet.any { !it.isDigit() }) return null
            val value = octet.toIntOrNull() ?: return null
            if (value > MAX_OCTET_VALUE) return null
            bytes[index] = value.toByte()
        }
        return bytes
    }

    @Suppress("ReturnCount")
    private fun parseIpv6(host: String): ByteArray? {
        if (!host.contains(':')) return null
        // Three colons in a row is never valid, and "::" may appear at most once.
        if (host.contains(":::")) return null
        if (countOccurrences(host, "::") > 1) return null

        // A trailing IPv4 literal ("::ffff:1.2.3.4") occupies the final four bytes.
        val lastColon = host.lastIndexOf(':')
        val tail = host.substring(lastColon + 1)
        val embeddedV4 = if (tail.contains('.')) parseIpv4(tail) ?: return null else null
        val hexPart = if (embeddedV4 != null) host.substring(0, lastColon) else host

        val groupBytes = IPV6_BYTES - (embeddedV4?.size ?: 0)
        val maxGroups = groupBytes / 2
        val (head, rear) = splitAroundCompression(hexPart) ?: return null

        val total = head.size + rear.size
        if (hexPart.contains("::")) {
            // "::" stands for at least one elided group.
            if (total >= maxGroups) return null
        } else {
            if (total != maxGroups) return null
        }

        val headBytes = head.toGroupBytes() ?: return null
        val rearBytes = rear.toGroupBytes() ?: return null

        val bytes = ByteArray(IPV6_BYTES)
        headBytes.copyInto(bytes, 0)
        rearBytes.copyInto(bytes, groupBytes - rearBytes.size)
        embeddedV4?.copyInto(bytes, groupBytes)
        return bytes
    }

    /**
     * Splits on the single "::" run, returning the groups before and after it. Without
     * compression everything lands in the head. An empty group anywhere (a stray leading or
     * trailing colon) is malformed.
     */
    private fun splitAroundCompression(hexPart: String): Pair<List<String>, List<String>>? {
        val index = hexPart.indexOf("::")
        if (index < 0) {
            val groups = hexPart.split(':')
            if (groups.any { it.isEmpty() }) return null
            return groups to emptyList()
        }
        val head = hexPart.substring(0, index).splitGroups() ?: return null
        val rear = hexPart.substring(index + 2).splitGroups() ?: return null
        return head to rear
    }

    /** Groups on one side of a "::"; empty side is fine, an empty group inside it is not. */
    private fun String.splitGroups(): List<String>? {
        if (isEmpty()) return emptyList()
        val groups = split(':')
        if (groups.any { it.isEmpty() }) return null
        return groups
    }

    private fun List<String>.toGroupBytes(): ByteArray? {
        val bytes = ByteArray(size * 2)
        for ((index, group) in withIndex()) {
            if (group.isEmpty() || group.length > MAX_GROUP_DIGITS) return null
            val value = group.toIntOrNull(HEX_RADIX) ?: return null
            bytes[index * 2] = (value shr Byte.SIZE_BITS).toByte()
            bytes[index * 2 + 1] = value.toByte()
        }
        return bytes
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        var count = 0
        var index = 0
        while (true) {
            val found = haystack.indexOf(needle, index)
            if (found < 0) return count
            count++
            index = found + needle.length
        }
    }

    private const val IPV4_OCTETS = 4
    private const val IPV6_BYTES = 16
    private const val MAX_OCTET_DIGITS = 3
    private const val MAX_OCTET_VALUE = 255
    private const val MAX_GROUP_DIGITS = 4
    private const val HEX_RADIX = 16
}
