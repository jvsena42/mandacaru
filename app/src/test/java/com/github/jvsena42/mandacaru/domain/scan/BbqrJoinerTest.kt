package com.github.jvsena42.mandacaru.domain.scan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class BbqrJoinerTest {

    @Test
    fun `single hex part completes`() {
        val bytes = byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte(), 0x01, 0x02, 0x03)

        val result = BbqrJoiner().addPart("B\$HP0100" + "70736274FF010203")

        assertTrue(result is BbqrJoiner.Result.Complete)
        assertArrayEquals(bytes, (result as BbqrJoiner.Result.Complete).data)
    }

    @Test
    fun `multi-part hex concatenates by index`() {
        val joiner = BbqrJoiner()

        val first = joiner.addPart("B\$HP0200" + "70736274")
        val second = joiner.addPart("B\$HP0201" + "FF010203")

        assertEquals(BbqrJoiner.Result.InProgress(received = 1, total = 2), first)
        assertTrue(second is BbqrJoiner.Result.Complete)
        assertArrayEquals(
            byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte(), 0x01, 0x02, 0x03),
            (second as BbqrJoiner.Result.Complete).data,
        )
    }

    @Test
    fun `base32 part decodes`() {
        val original = ByteArray(40) { (it * 3).toByte() }

        val result = BbqrJoiner().addPart("B\$2P0100" + base32Encode(original))

        assertTrue(result is BbqrJoiner.Result.Complete)
        assertArrayEquals(original, (result as BbqrJoiner.Result.Complete).data)
    }

    @Test
    fun `zlib part inflates the original bytes`() {
        val original = ByteArray(220) { (it % 17).toByte() }

        val result = BbqrJoiner().addPart("B\$ZP0100" + base32Encode(rawDeflate(original)))

        assertTrue(result is BbqrJoiner.Result.Complete)
        assertArrayEquals(original, (result as BbqrJoiner.Result.Complete).data)
    }

    @Test
    fun `part from a different sequence is rejected`() {
        val joiner = BbqrJoiner()
        joiner.addPart("B\$HP0200" + "7073")

        try {
            joiner.addPart("B\$HP0300" + "6274")
            fail("expected a mismatched-sequence error")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("different sequence") == true)
        }
    }

    private fun rawDeflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            output.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return output.toByteArray()
    }

    private fun base32Encode(data: ByteArray): String {
        val builder = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in data) {
            buffer = (buffer shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK)
            bits += BITS_PER_BYTE
            while (bits >= BITS_PER_CHAR) {
                bits -= BITS_PER_CHAR
                builder.append(ALPHABET[(buffer ushr bits) and CHAR_MASK])
            }
        }
        if (bits > 0) {
            builder.append(ALPHABET[(buffer shl (BITS_PER_CHAR - bits)) and CHAR_MASK])
        }
        return builder.toString()
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        const val BITS_PER_BYTE = 8
        const val BITS_PER_CHAR = 5
        const val BYTE_MASK = 0xFF
        const val CHAR_MASK = 0x1F
    }
}
