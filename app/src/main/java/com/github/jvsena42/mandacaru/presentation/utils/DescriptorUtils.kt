package com.github.jvsena42.mandacaru.presentation.utils

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object DescriptorUtils {

    private const val VERSION_PREFIX_LENGTH = 4
    private const val CHECKSUM_LENGTH = 4
    private const val BASE58_RADIX = 58L

    private val PRIVATE_KEY_PREFIXES = listOf("xprv", "yprv", "zprv", "tprv", "uprv", "vprv")

    private val SCRIPT_FUNCTIONS = listOf(
        "wpkh(", "wsh(", "sh(", "tr(", "pkh(", "combo(", "addr(", "raw(", "multi(", "sortedmulti(",
    )
    private val EXTENDED_KEY_PREFIXES = listOf(
        "xpub", "ypub", "zpub", "tpub", "upub", "vpub",
        "Ypub", "Zpub", "Upub", "Vpub",
    )
    private val JSON_DESCRIPTOR_KEYS = listOf("descriptor", "desc", "output_descriptor")

    private val POLICY_REGEX = Regex("""(\d+)\s+of\s+\d+""", RegexOption.IGNORE_CASE)
    private val POLICY_LINE_REGEX = Regex("""(?im)^\s*Policy\s*:""")
    private val FORMAT_LINE_REGEX = Regex("""(?im)^\s*Format\s*:""")
    private val FINGERPRINT_REGEX = Regex("""[0-9a-fA-F]{8}""")
    private val KEY_ORIGIN_REGEX = Regex("""\[([0-9a-fA-F]{8})(/[^\]]*)?]""")
    private val MULTISIG_REGEX = Regex("""sortedmulti\((\d+),""")

    // SLIP-132 version bytes for public keys
    private val VERSION_MAGIC_XPUB = byteArrayOf(0x04, 0x88.toByte(), 0xB2.toByte(), 0x1E)
    private val VERSION_MAGIC_YPUB = byteArrayOf(0x04, 0x9D.toByte(), 0x7C, 0xB2.toByte())
    private val VERSION_MAGIC_ZPUB = byteArrayOf(0x04, 0xB2.toByte(), 0x47, 0x46)
    private val VERSION_MAGIC_TPUB = byteArrayOf(0x04, 0x35, 0x87.toByte(), 0xCF.toByte())
    private val VERSION_MAGIC_UPUB = byteArrayOf(0x04, 0x4A, 0x52, 0x62)
    private val VERSION_MAGIC_VPUB = byteArrayOf(0x04, 0x5F, 0x1C, 0xF6.toByte())

    // SLIP-132 multisig version bytes (capitalized Ypub/Zpub/Upub/Vpub, as exported by
    // BlueWallet's coordination setup and Coldcard multisig files).
    private val VERSION_MAGIC_YPUB_MULTI = byteArrayOf(0x02, 0x95.toByte(), 0xB4.toByte(), 0x3F)
    private val VERSION_MAGIC_ZPUB_MULTI = byteArrayOf(0x02, 0xAA.toByte(), 0x7E, 0xD3.toByte())
    private val VERSION_MAGIC_UPUB_MULTI = byteArrayOf(0x02, 0x42, 0x89.toByte(), 0xEF.toByte())
    private val VERSION_MAGIC_VPUB_MULTI = byteArrayOf(0x02, 0x57, 0x54, 0x83.toByte())

    private val SLIP132_TO_STANDARD = mapOf(
        VERSION_MAGIC_YPUB.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_ZPUB.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_UPUB.toList() to VERSION_MAGIC_TPUB,
        VERSION_MAGIC_VPUB.toList() to VERSION_MAGIC_TPUB,
        VERSION_MAGIC_YPUB_MULTI.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_ZPUB_MULTI.toList() to VERSION_MAGIC_XPUB,
        VERSION_MAGIC_UPUB_MULTI.toList() to VERSION_MAGIC_TPUB,
        VERSION_MAGIC_VPUB_MULTI.toList() to VERSION_MAGIC_TPUB,
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

    /** A human-readable summary of a descriptor, used by the scan confirmation dialog. */
    data class DescriptorSummary(
        val scriptType: String,
        val fingerprint: String? = null,
        val derivationPath: String? = null,
        val multisig: String? = null,
    )

    /**
     * Pulls a descriptor out of a scanned/pasted QR payload. Handles three shapes, in order:
     * a multisig "setup file" (BlueWallet/Coldcard/Krux), a JSON wallet export
     * (Sparrow/Specter), or a raw descriptor / extended key. Returns null when nothing
     * descriptor-like is found.
     */
    fun extractDescriptor(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (looksLikeMultisigSetupFile(trimmed)) return parseMultisigSetupFile(trimmed)
        extractDescriptorFromJson(trimmed)?.let { return it }
        return if (isDescriptorLike(trimmed)) trimmed else null
    }

    /**
     * Assembles a `sortedmulti` descriptor from a BlueWallet / Coldcard multisig setup
     * file. The file is a list of `Key: value` lines plus one `<fingerprint>: <xpub>` line
     * per cosigner. SLIP-132 keys are normalized and BIP-67 ordering is delegated to the
     * `sortedmulti` descriptor function, so cosigner order is preserved as written.
     */
    @Suppress("ReturnCount")
    fun parseMultisigSetupFile(text: String): String? {
        var threshold: Int? = null
        var derivation = ""
        var format = "P2WSH"
        val keys = mutableListOf<String>()

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            val colon = line.indexOf(':')
            if (line.isEmpty() || line.startsWith("#") || colon <= 0) continue
            val label = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()

            when {
                label.equals("Policy", ignoreCase = true) ->
                    threshold = POLICY_REGEX.find(value)?.groupValues?.get(1)?.toIntOrNull()
                label.equals("Derivation", ignoreCase = true) -> derivation = value
                label.equals("Format", ignoreCase = true) -> format = value
                label.equals("Name", ignoreCase = true) -> Unit
                FINGERPRINT_REGEX.matches(label) ->
                    keys += buildCosignerKey(label, derivation, value)
            }
        }

        val m = threshold ?: return null
        if (keys.isEmpty()) return null
        val inner = "sortedmulti($m,${keys.joinToString(",")})"
        return when (format.uppercase()) {
            "P2SH-P2WSH", "P2WSH-P2SH" -> "sh(wsh($inner))"
            "P2SH" -> "sh($inner)"
            else -> "wsh($inner)"
        }
    }

    fun summarize(descriptor: String): DescriptorSummary {
        val origin = KEY_ORIGIN_REGEX.find(descriptor)
        val fingerprint = origin?.groupValues?.get(1)
        val path = origin?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }?.removePrefix("/")
        val multi = MULTISIG_REGEX.find(descriptor)?.let { match ->
            val m = match.groupValues[1]
            val n = descriptor.split("]").size - 1
            if (n > 0) "$m-of-$n" else null
        }
        return DescriptorSummary(
            scriptType = scriptTypeOf(descriptor),
            fingerprint = fingerprint,
            derivationPath = path,
            multisig = multi,
        )
    }

    private fun scriptTypeOf(descriptor: String): String = when {
        descriptor.startsWith("sh(wsh(sortedmulti") || descriptor.startsWith("sh(wsh(multi") ->
            "Multisig Nested SegWit (P2SH-P2WSH)"
        descriptor.startsWith("wsh(sortedmulti") || descriptor.startsWith("wsh(multi") ->
            "Multisig Native SegWit (P2WSH)"
        descriptor.startsWith("sh(sortedmulti") || descriptor.startsWith("sh(multi") ->
            "Multisig Legacy (P2SH)"
        descriptor.startsWith("sh(wpkh(") -> "Nested SegWit (P2SH-P2WPKH)"
        descriptor.startsWith("wpkh(") -> "Native SegWit (P2WPKH)"
        descriptor.startsWith("tr(") -> "Taproot (P2TR)"
        descriptor.startsWith("wsh(") -> "Native SegWit (P2WSH)"
        descriptor.startsWith("pkh(") -> "Legacy (P2PKH)"
        else -> "Unknown"
    }

    private fun buildCosignerKey(fingerprint: String, derivation: String, key: String): String {
        val standardKey = convertSlip132ToStandard(key)
        val origin = derivation
            .removePrefix("m")
            .removePrefix("/")
            .replace("'", "h")
        val prefix = if (origin.isEmpty()) {
            "[${fingerprint.lowercase()}]"
        } else {
            "[${fingerprint.lowercase()}/$origin]"
        }
        return "$prefix$standardKey/<0;1>/*"
    }

    private fun looksLikeMultisigSetupFile(text: String): Boolean =
        text.contains("Multisig setup file", ignoreCase = true) ||
            (FORMAT_LINE_REGEX.containsMatchIn(text) && POLICY_LINE_REGEX.containsMatchIn(text))

    @Suppress("ReturnCount")
    private fun extractDescriptorFromJson(text: String): String? {
        if (!text.startsWith("{")) return null
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        for (key in JSON_DESCRIPTOR_KEYS) {
            val value = root.optString(key, "")
            if (value.isNotEmpty() && isDescriptorLike(value)) return value
        }
        return findDescriptorInJson(root)
    }

    private fun findDescriptorInJson(node: Any?): String? = when (node) {
        is String -> node.takeIf { isDescriptorLike(it) }
        is JSONObject -> node.keys().asSequence()
            .firstNotNullOfOrNull { findDescriptorInJson(node.get(it)) }
        is JSONArray -> (0 until node.length())
            .firstNotNullOfOrNull { findDescriptorInJson(node.get(it)) }
        else -> null
    }

    private fun isDescriptorLike(value: String): Boolean {
        val candidate = value.trim()
        return SCRIPT_FUNCTIONS.any { candidate.startsWith(it) } ||
            EXTENDED_KEY_PREFIXES.any { candidate.startsWith(it) }
    }

    @Suppress("ReturnCount")
    internal fun convertSlip132ToStandard(key: String): String {
        val decoded = base58CheckDecode(key) ?: return key

        if (decoded.size < VERSION_PREFIX_LENGTH) return key

        val prefix = decoded.sliceArray(0 until VERSION_PREFIX_LENGTH).toList()
        val standardPrefix = SLIP132_TO_STANDARD[prefix] ?: return key

        standardPrefix.copyInto(decoded, 0)
        return base58CheckEncode(decoded)
    }

    // --- Base58Check ---

    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    @Suppress("ReturnCount")
    private fun base58CheckDecode(input: String): ByteArray? {
        val decoded = base58Decode(input) ?: return null
        if (decoded.size < CHECKSUM_LENGTH) return null

        val payload = decoded.sliceArray(0 until decoded.size - CHECKSUM_LENGTH)
        val checksum = decoded.sliceArray(decoded.size - CHECKSUM_LENGTH until decoded.size)
        val expectedChecksum = doubleSha256(payload).sliceArray(0 until CHECKSUM_LENGTH)

        return if (checksum.contentEquals(expectedChecksum)) payload else null
    }

    private fun base58CheckEncode(payload: ByteArray): String {
        val checksum = doubleSha256(payload).sliceArray(0 until CHECKSUM_LENGTH)
        return base58Encode(payload + checksum)
    }

    private fun base58Decode(input: String): ByteArray? {
        var result = java.math.BigInteger.ZERO
        for (c in input) {
            val digit = BASE58_ALPHABET.indexOf(c)
            if (digit < 0) return null
            result = result.multiply(java.math.BigInteger.valueOf(BASE58_RADIX)) + java.math.BigInteger.valueOf(digit.toLong())
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
        val fiftyEight = java.math.BigInteger.valueOf(BASE58_RADIX)

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
