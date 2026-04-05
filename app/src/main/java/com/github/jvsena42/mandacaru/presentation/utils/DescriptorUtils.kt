package com.github.jvsena42.mandacaru.presentation.utils

import java.security.MessageDigest

object DescriptorUtils {

    private val PRIVATE_KEY_PREFIXES = listOf("xprv", "yprv", "zprv", "tprv", "uprv", "vprv")

    // SLIP-132 version bytes for public keys
    private val VERSION_MAGIC_XPUB = byteArrayOf(0x04, 0x88.toByte(), 0xB2.toByte(), 0x1E)
    private val VERSION_MAGIC_YPUB = byteArrayOf(0x04, 0x9D.toByte(), 0x7C, 0xB2.toByte())
    private val VERSION_MAGIC_ZPUB = byteArrayOf(0x04, 0xB2.toByte(), 0x47, 0x46)
    private val VERSION_MAGIC_TPUB = byteArrayOf(0x04, 0x35, 0x87.toByte(), 0xCF.toByte())
    private val VERSION_MAGIC_UPUB = byteArrayOf(0x04, 0x4A, 0x52, 0x62)
    private val VERSION_MAGIC_VPUB = byteArrayOf(0x04, 0x5F, 0x1C, 0xF6.toByte())

    private val SLIP132_TO_STANDARD = mapOf(
        VERSION_MAGIC_YPUB.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_ZPUB.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_UPUB.toList() to VERSION_MAGIC_TPUB,
        VERSION_MAGIC_VPUB.toList() to VERSION_MAGIC_TPUB,
    )

    fun wrapDescriptorIfNeeded(input: String): String {
        if (input.contains("(")) return input

        val converted = convertSlip132ToStandard(input)
        val keyWithPath = if (converted.contains("/")) converted else "$converted/<0;1>/*"

        return when {
            input.startsWith("zpub") || input.startsWith("vpub") -> "wpkh($keyWithPath)"
            input.startsWith("ypub") || input.startsWith("upub") -> "sh(wpkh($keyWithPath))"
            input.startsWith("xpub") || input.startsWith("tpub") -> "pkh($keyWithPath)"
            else -> input
        }
    }

    fun isPrivateKey(input: String): Boolean {
        return PRIVATE_KEY_PREFIXES.any { input.startsWith(it) }
    }

    internal fun convertSlip132ToStandard(key: String): String {
        val decoded = base58CheckDecode(key) ?: return key
        if (decoded.size < 4) return key

        val prefix = decoded.sliceArray(0 until 4).toList()
        val standardPrefix = SLIP132_TO_STANDARD[prefix] ?: return key

        standardPrefix.copyInto(decoded, 0)
        return base58CheckEncode(decoded)
    }

    // --- Base58Check ---

    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    private fun base58CheckDecode(input: String): ByteArray? {
        val decoded = base58Decode(input) ?: return null
        if (decoded.size < 4) return null

        val payload = decoded.sliceArray(0 until decoded.size - 4)
        val checksum = decoded.sliceArray(decoded.size - 4 until decoded.size)
        val expectedChecksum = doubleSha256(payload).sliceArray(0 until 4)

        return if (checksum.contentEquals(expectedChecksum)) payload else null
    }

    private fun base58CheckEncode(payload: ByteArray): String {
        val checksum = doubleSha256(payload).sliceArray(0 until 4)
        return base58Encode(payload + checksum)
    }

    private fun base58Decode(input: String): ByteArray? {
        var result = java.math.BigInteger.ZERO
        for (c in input) {
            val digit = BASE58_ALPHABET.indexOf(c)
            if (digit < 0) return null
            result = result.multiply(java.math.BigInteger.valueOf(58)) + java.math.BigInteger.valueOf(digit.toLong())
        }

        val bytes = result.toByteArray()
        // BigInteger may add a leading zero byte for sign; strip it
        val stripped = if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.drop(1).toByteArray() else bytes

        // Count leading '1's in input — each represents a leading zero byte
        val leadingZeros = input.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + stripped
    }

    private fun base58Encode(data: ByteArray): String {
        var num = java.math.BigInteger(1, data)
        val sb = StringBuilder()
        val fiftyEight = java.math.BigInteger.valueOf(58)

        while (num > java.math.BigInteger.ZERO) {
            val (quotient, remainder) = num.divideAndRemainder(fiftyEight)
            sb.append(BASE58_ALPHABET[remainder.toInt()])
            num = quotient
        }

        // Preserve leading zero bytes as '1'
        for (b in data) {
            if (b == 0.toByte()) sb.append('1') else break
        }

        return sb.reverse().toString()
    }

    private fun doubleSha256(data: ByteArray): ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        return sha256.digest(sha256.digest(data))
    }
}
