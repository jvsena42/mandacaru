package com.github.jvsena42.mandacaru.presentation.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotCodecTest {

    private fun sampleJson(
        network: String = "bitcoin",
        height: Long = 939969L,
        leaves: Long = 2_345_678_901L,
        blockHash: String = "000000000000000000009d36aae180d04aeac872adb14e22f65c8b6647a8bf79",
        roots: List<String> = listOf(
            "08daaf0c6bc41531885cfcfdeb89c34bd4d06ab4b105cf0e81bd74ab082693f5",
            "8d4166d0303d41f7023cd35b95b24455b99b2f4a2728083bba3d172727900bed",
            "08d95bc9b7bc0bc07c9f626322c0092bd16c198fcb96d290fe1a191e9719b4c9",
        ),
    ): String = JSONObject().apply {
        put("version", 1)
        put("network", network)
        put("block_hash", blockHash)
        put("height", height)
        put("leaves", leaves)
        put("roots", JSONArray().apply { roots.forEach { put(it) } })
    }.toString()

    private fun assertJsonEquivalent(expected: String, actual: String) {
        val a = JSONObject(expected)
        val b = JSONObject(actual)
        assertEquals(a.getInt("version"), b.getInt("version"))
        assertEquals(a.getString("network"), b.getString("network"))
        assertEquals(a.getString("block_hash"), b.getString("block_hash"))
        assertEquals(a.getLong("height"), b.getLong("height"))
        assertEquals(a.getLong("leaves"), b.getLong("leaves"))
        val ar = a.getJSONArray("roots")
        val br = b.getJSONArray("roots")
        assertEquals(ar.length(), br.length())
        for (i in 0 until ar.length()) {
            assertEquals(ar.getString(i), br.getString(i))
        }
    }

    @Test
    fun `round-trip preserves every field for bitcoin`() {
        val json = sampleJson()
        val compact = SnapshotCodec.encodeCompact(json)
        val back = SnapshotCodec.normalizeToJson(compact)
        assertJsonEquivalent(json, back)
    }

    @Test
    fun `round-trip preserves every field for signet`() {
        val json = sampleJson(
            network = "signet",
            height = 200_000L,
            leaves = 123_456L,
            blockHash = "00000000aabbccddeeff00112233445566778899aabbccddeeff001122334455",
            roots = listOf(
                "0000000000000000000000000000000000000000000000000000000000000001",
            ),
        )
        val compact = SnapshotCodec.encodeCompact(json)
        val back = SnapshotCodec.normalizeToJson(compact)
        assertJsonEquivalent(json, back)
    }

    @Test
    fun `round-trip handles regtest with empty roots`() {
        val json = sampleJson(
            network = "regtest",
            height = 0L,
            leaves = 0L,
            blockHash = "0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206",
            roots = emptyList(),
        )
        val compact = SnapshotCodec.encodeCompact(json)
        val back = SnapshotCodec.normalizeToJson(compact)
        assertJsonEquivalent(json, back)
    }

    @Test
    fun `encodeCompact emits uppercase UTREEXO1 prefix`() {
        val compact = SnapshotCodec.encodeCompact(sampleJson())
        assertTrue(compact.startsWith("UTREEXO1"))
        // Canonical: entire string is uppercase.
        assertEquals(compact, compact.uppercase())
    }

    @Test
    fun `size sanity — 21 roots fits well under 1200 chars`() {
        val roots = (0 until 21).map { String.format("%064x", it.toLong()) }
        val compact = SnapshotCodec.encodeCompact(sampleJson(roots = roots))
        assertTrue("compact length ${compact.length}", compact.length <= 1200)
    }

    @Test
    fun `legacy JSON passes through unchanged`() {
        val json = sampleJson()
        val normalized = SnapshotCodec.normalizeToJson(json)
        assertEquals(json, normalized)
    }

    @Test
    fun `decoder accepts lowercase canonical form`() {
        val compact = SnapshotCodec.encodeCompact(sampleJson()).lowercase()
        val back = SnapshotCodec.normalizeToJson(compact)
        assertJsonEquivalent(sampleJson(), back)
    }

    @Test
    fun `mixed case is rejected`() {
        val compact = SnapshotCodec.encodeCompact(sampleJson())
        val mixed = compact.substring(0, 8) + compact.substring(8).lowercase()
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SnapshotCodec.normalizeToJson(mixed)
        }
        assertTrue(ex.message!!.contains("Mixed"))
    }

    @Test
    fun `checksum mismatch is rejected`() {
        val compact = SnapshotCodec.encodeCompact(sampleJson())
        // Mutate a data char in the middle.
        val mid = compact.length / 2
        val mutated = buildString {
            append(compact.substring(0, mid))
            // Pick a different valid charset char.
            val original = compact[mid]
            val alt = if (original == 'Q') 'P' else 'Q'
            append(alt)
            append(compact.substring(mid + 1))
        }
        assertNotEquals(compact, mutated)
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotCodec.normalizeToJson(mutated)
        }
    }

    @Test
    fun `non-utreexo HRP falls through as pass-through — not a codec error`() {
        // Strings that don't start with `utreexo1` are treated as legacy JSON
        // and returned unchanged. They'll fail later at the FFI validator.
        val notUtreexo = "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"
        assertEquals(notUtreexo, SnapshotCodec.normalizeToJson(notUtreexo))
    }

    @Test
    fun `UTREEXO prefix but malformed body throws`() {
        // Starts with the expected HRP, so the codec attempts to decode —
        // but the content is garbage, so it must fail fast rather than pass through.
        val malformed = "UTREEXO1QQQQQQQQQQQQ"
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotCodec.normalizeToJson(malformed)
        }
    }

    @Test
    fun `isCompact recognises prefix`() {
        val compact = SnapshotCodec.encodeCompact(sampleJson())
        assertTrue(SnapshotCodec.isCompact(compact))
        assertTrue(SnapshotCodec.isCompact(compact.lowercase()))
        assertTrue(SnapshotCodec.isCompact("  $compact  "))
        assertFalse(SnapshotCodec.isCompact(sampleJson()))
        assertFalse(SnapshotCodec.isCompact("bc1qabc"))
    }

    @Test
    fun `unknown network name during encode is rejected`() {
        val json = sampleJson(network = "mainnet-v2")
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotCodec.encodeCompact(json)
        }
    }
}
