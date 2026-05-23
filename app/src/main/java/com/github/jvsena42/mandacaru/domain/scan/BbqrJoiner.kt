package com.github.jvsena42.mandacaru.domain.scan

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Pure-Kotlin joiner for animated BBQr sequences (https://bbqr.org). Each scanned part carries an
 * 8-char header `B$` + encoding + file type + base36 total + base36 index, followed by the encoded
 * chunk. Chunks are concatenated by index and decoded once the whole sequence has arrived. Kept
 * dependency-free on purpose so there is no native library to keep 16 KB-page aligned.
 */
class BbqrJoiner {

    sealed interface Result {
        data class InProgress(val received: Int, val total: Int) : Result
        class Complete(val data: ByteArray) : Result
    }

    private var encoding: Char? = null
    private var fileType: Char? = null
    private var total = 0
    private val chunks = HashMap<Int, String>()

    fun addPart(raw: String): Result {
        require(raw.length >= HEADER_LENGTH && raw.startsWith(PREFIX)) { "Not a BBQr part" }
        val partEncoding = raw[ENCODING_INDEX]
        val partType = raw[FILE_TYPE_INDEX]
        val partTotal = raw.substring(TOTAL_START, TOTAL_END).toInt(BASE36)
        val index = raw.substring(INDEX_START, HEADER_LENGTH).toInt(BASE36)

        require(partTotal in 1..MAX_PARTS) { "Invalid BBQr part count" }
        require(index in 0 until partTotal) { "Invalid BBQr part index" }

        if (encoding == null) {
            encoding = partEncoding
            fileType = partType
            total = partTotal
        } else {
            require(partEncoding == encoding && partType == fileType && partTotal == total) {
                "BBQr part from a different sequence"
            }
        }

        chunks[index] = raw.substring(HEADER_LENGTH)
        return if (chunks.size < total) {
            Result.InProgress(chunks.size, total)
        } else {
            Result.Complete(decodeJoined())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decodeJoined(): ByteArray {
        val joined = buildString { for (i in 0 until total) append(chunks.getValue(i)) }
        return when (encoding) {
            ENCODING_HEX -> joined.lowercase().hexToByteArray()
            ENCODING_BASE32 -> Base32.decode(joined)
            ENCODING_ZLIB -> inflateRaw(Base32.decode(joined))
            else -> throw IllegalArgumentException("Unsupported BBQr encoding: $encoding")
        }
    }

    private fun inflateRaw(data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(data)
        val output = ByteArrayOutputStream(data.size * INFLATE_GROWTH)
        val buffer = ByteArray(INFLATE_BUFFER)
        try {
            var produced = inflater.inflate(buffer)
            while (produced > 0) {
                output.write(buffer, 0, produced)
                produced = inflater.inflate(buffer)
            }
        } finally {
            inflater.end()
        }
        return output.toByteArray()
    }

    private companion object {
        const val PREFIX = "B$"
        const val HEADER_LENGTH = 8
        const val ENCODING_INDEX = 2
        const val FILE_TYPE_INDEX = 3
        const val TOTAL_START = 4
        const val TOTAL_END = 6
        const val INDEX_START = 6
        const val BASE36 = 36
        const val MAX_PARTS = 1295
        const val ENCODING_HEX = 'H'
        const val ENCODING_BASE32 = '2'
        const val ENCODING_ZLIB = 'Z'
        const val INFLATE_BUFFER = 4096
        const val INFLATE_GROWTH = 4
    }
}

internal object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val BITS_PER_CHAR = 5
    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFF

    fun decode(input: String): ByteArray {
        val output = ByteArrayOutputStream(input.length * BITS_PER_CHAR / BITS_PER_BYTE)
        var buffer = 0
        var bits = 0
        for (symbol in input) {
            if (symbol == '=') continue
            val value = ALPHABET.indexOf(symbol.uppercaseChar())
            require(value >= 0) { "Invalid base32 character: $symbol" }
            buffer = (buffer shl BITS_PER_CHAR) or value
            bits += BITS_PER_CHAR
            if (bits >= BITS_PER_BYTE) {
                bits -= BITS_PER_BYTE
                output.write((buffer ushr bits) and BYTE_MASK)
            }
        }
        return output.toByteArray()
    }
}
