package com.github.jvsena42.mandacaru.presentation.utils

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bech32m-based compact encoding for Utreexo snapshots.
 *
 * Canonical on-the-wire form is the Rust-side JSON emitted by
 * `Florestad.dumpUtreexoState()`. This codec wraps that JSON into a smaller
 * `UTREEXO1…` bech32m blob for QR / clipboard / share, and transparently
 * unwraps on import so the FFI validator always receives JSON.
 *
 * See the plan file for the wire-format rationale.
 */
object SnapshotCodec {
    private const val HRP = "utreexo"
    private const val BECH32M_CONST: Int = 0x2bc830a3
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    private const val FORMAT_VERSION: Byte = 1
    private const val MIN_BODY_SIZE = 47 // up to and including root_count

    fun isCompact(payload: String): Boolean =
        payload.trim().lowercase().startsWith("${HRP}1")

    fun encodeCompact(json: String): String {
        val body = packBody(json)
        val data5 = convertBits(body, 8, 5, pad = true)
        return bech32mEncode(HRP, data5).uppercase()
    }

    fun normalizeToJson(payload: String): String {
        val trimmed = payload.trim()
        if (!isCompact(trimmed)) return trimmed
        val (_, data5) = bech32mDecode(trimmed)
        val body = convertBits5to8(data5)
        return unpackBody(body)
    }

    // ---------------------------------------------------------------------
    // Binary body <-> JSON
    // ---------------------------------------------------------------------

    private fun packBody(json: String): ByteArray {
        val obj = JSONObject(json)
        val network = obj.getString(KEY_NETWORK)
        val networkByte = networkNameToByte(network)
            ?: throw IllegalArgumentException("Unknown network: $network")
        val height = obj.getLong(KEY_HEIGHT)
        if (height !in 0L..UINT32_MAX) {
            throw IllegalArgumentException("height out of u32 range: $height")
        }
        val leaves = obj.getLong(KEY_LEAVES)
        if (leaves < 0L) {
            throw IllegalArgumentException("leaves out of u64 range (negative Long): $leaves")
        }
        val blockHashHex = obj.getString(KEY_BLOCK_HASH)
        val blockHashBytes = hexToBytes(blockHashHex)
        require(blockHashBytes.size == 32) { "block_hash must be 32 bytes, got ${blockHashBytes.size}" }

        val rootsJson = obj.getJSONArray(KEY_ROOTS)
        require(rootsJson.length() in 0..MAX_ROOTS) {
            "roots length ${rootsJson.length()} out of range [0,$MAX_ROOTS]"
        }
        val roots = Array(rootsJson.length()) { i ->
            val rBytes = hexToBytes(rootsJson.getString(i))
            require(rBytes.size == 32) { "root[$i] must be 32 bytes" }
            rBytes
        }

        val buf = ByteBuffer
            .allocate(MIN_BODY_SIZE + 32 * roots.size)
            .order(ByteOrder.LITTLE_ENDIAN)
        buf.put(FORMAT_VERSION)
        buf.put(networkByte)
        buf.putInt(height.toInt()) // u32 LE — mask happens via Int width
        buf.putLong(leaves)
        buf.put(blockHashBytes)
        buf.put(roots.size.toByte())
        roots.forEach { buf.put(it) }
        return buf.array()
    }

    private fun unpackBody(body: ByteArray): String {
        require(body.size >= MIN_BODY_SIZE) { "Compact body too short: ${body.size} < $MIN_BODY_SIZE" }
        val buf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
        val version = buf.get()
        if (version != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported snapshot format version: $version")
        }
        val networkByte = buf.get()
        val network = byteToNetworkName(networkByte)
            ?: throw IllegalArgumentException("Unknown network tag byte: $networkByte")
        val height = (buf.int.toLong() and 0xFFFF_FFFFL)
        val leaves = buf.long
        val blockHashBytes = ByteArray(32).also { buf.get(it) }
        val rootCount = buf.get().toInt() and 0xFF
        val remaining = body.size - MIN_BODY_SIZE
        require(remaining == rootCount * 32) {
            "root_count=$rootCount disagrees with remaining body bytes ($remaining)"
        }
        val roots = Array(rootCount) {
            val r = ByteArray(32)
            buf.get(r)
            bytesToHex(r)
        }

        val rootsJson = org.json.JSONArray()
        roots.forEach { rootsJson.put(it) }

        return JSONObject().apply {
            put(KEY_VERSION, version.toInt())
            put(KEY_NETWORK, network)
            put(KEY_BLOCK_HASH, bytesToHex(blockHashBytes))
            put(KEY_HEIGHT, height)
            put(KEY_LEAVES, leaves)
            put(KEY_ROOTS, rootsJson)
        }.toString()
    }

    private fun networkNameToByte(name: String): Byte? = when (name) {
        "bitcoin" -> 0
        "signet" -> 1
        "testnet" -> 2
        "testnet4" -> 3
        "regtest" -> 4
        else -> null
    }

    private fun byteToNetworkName(b: Byte): String? = when (b.toInt() and 0xFF) {
        0 -> "bitcoin"
        1 -> "signet"
        2 -> "testnet"
        3 -> "testnet4"
        4 -> "regtest"
        else -> null
    }

    // ---------------------------------------------------------------------
    // bech32m
    // ---------------------------------------------------------------------

    private val CHARSET_REV: IntArray = IntArray(128) { -1 }.also { arr ->
        CHARSET.forEachIndexed { i, c -> arr[c.code] = i }
    }

    private val GEN = intArrayOf(
        0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3,
    )

    private fun bech32mEncode(hrp: String, data5: IntArray): String {
        val checksum = createChecksum(hrp, data5)
        val sb = StringBuilder(hrp.length + 1 + data5.size + checksum.size)
        sb.append(hrp).append('1')
        data5.forEach { sb.append(CHARSET[it]) }
        checksum.forEach { sb.append(CHARSET[it]) }
        return sb.toString()
    }

    private fun bech32mDecode(s: String): Pair<String, IntArray> {
        val upper = s.any { it.isUpperCase() }
        val lower = s.any { it.isLowerCase() }
        if (upper && lower) {
            throw IllegalArgumentException("Mixed-case bech32 string")
        }
        val lowered = s.lowercase()
        val sepPos = lowered.lastIndexOf('1')
        require(sepPos >= 1) { "Missing HRP separator" }
        val hrp = lowered.substring(0, sepPos)
        val dataPart = lowered.substring(sepPos + 1)
        require(hrp == HRP) { "Unexpected HRP: $hrp" }
        require(dataPart.length >= 6) { "Data part too short for checksum" }
        dataPart.forEach { c ->
            val v = if (c.code < 128) CHARSET_REV[c.code] else -1
            require(v >= 0) { "Invalid bech32 character: '$c'" }
        }
        val values = IntArray(dataPart.length) { CHARSET_REV[dataPart[it].code] }
        require(verifyChecksum(hrp, values)) { "Bech32m checksum mismatch" }
        return hrp to values.copyOfRange(0, values.size - 6)
    }

    private fun createChecksum(hrp: String, data: IntArray): IntArray {
        val values = hrpExpand(hrp) + data + IntArray(6)
        val polymod = polymod(values) xor BECH32M_CONST
        return IntArray(6) { i ->
            (polymod shr (5 * (5 - i))) and 31
        }
    }

    private fun verifyChecksum(hrp: String, data: IntArray): Boolean =
        polymod(hrpExpand(hrp) + data) == BECH32M_CONST

    private fun hrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) out[i] = hrp[i].code shr 5
        out[hrp.length] = 0
        for (i in hrp.indices) out[hrp.length + 1 + i] = hrp[i].code and 31
        return out
    }

    private fun polymod(values: IntArray): Int {
        var chk = 1
        for (v in values) {
            val b = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0..4) {
                if (((b ushr i) and 1) != 0) chk = chk xor GEN[i]
            }
        }
        return chk
    }

    // ---------------------------------------------------------------------
    // bit conversion
    // ---------------------------------------------------------------------

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): IntArray {
        val ints = IntArray(data.size) { data[it].toInt() and 0xFF }
        return convertBits(ints, fromBits, toBits, pad)
    }

    private fun convertBits(data: IntArray, fromBits: Int, toBits: Int, pad: Boolean): IntArray {
        var acc = 0
        var bits = 0
        val out = ArrayList<Int>(data.size * fromBits / toBits + 1)
        val maxv = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (value in data) {
            require(value >= 0 && (value ushr fromBits) == 0) { "value out of range in convertBits" }
            acc = ((acc shl fromBits) or value) and maxAcc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.add((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) out.add((acc shl (toBits - bits)) and maxv)
        } else {
            require(bits < fromBits && ((acc shl (toBits - bits)) and maxv) == 0) {
                "Non-zero padding in bit conversion"
            }
        }
        return out.toIntArray()
    }

    // Overload: IntArray → ByteArray (for 5→8 bit round-trip)
    private fun convertBits(data: IntArray, fromBits: Int, toBits: Int, pad: Boolean, asBytes: Boolean): ByteArray {
        val ints = convertBits(data, fromBits, toBits, pad)
        return ByteArray(ints.size) { ints[it].toByte() }
    }

    // Public alias used by unpackBody — 5→8 bit conversion that returns bytes.
    // (Avoids overload ambiguity on the call site.)
    private fun convertBits5to8(data: IntArray): ByteArray =
        convertBits(data, 5, 8, pad = false, asBytes = true)

    // ---------------------------------------------------------------------
    // hex
    // ---------------------------------------------------------------------

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string has odd length" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hexDigit(hex[i * 2])
            val lo = hexDigit(hex[i * 2 + 1])
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun hexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> 10 + (c - 'a')
        in 'A'..'F' -> 10 + (c - 'A')
        else -> throw IllegalArgumentException("Invalid hex digit: '$c'")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_ALPHABET[v ushr 4])
            sb.append(HEX_ALPHABET[v and 0x0F])
        }
        return sb.toString()
    }

    private const val HEX_ALPHABET = "0123456789abcdef"
    private const val UINT32_MAX = 0xFFFF_FFFFL
    private const val MAX_ROOTS = 63

    private const val KEY_VERSION = "version"
    private const val KEY_NETWORK = "network"
    private const val KEY_BLOCK_HASH = "block_hash"
    private const val KEY_HEIGHT = "height"
    private const val KEY_LEAVES = "leaves"
    private const val KEY_ROOTS = "roots"
}
