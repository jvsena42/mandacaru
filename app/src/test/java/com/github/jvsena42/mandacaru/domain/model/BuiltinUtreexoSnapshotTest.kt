package com.github.jvsena42.mandacaru.domain.model

import com.github.jvsena42.mandacaru.presentation.utils.SnapshotCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinUtreexoSnapshotTest {

    private val constant: String = Constants.BUILTIN_UTREEXO_SNAPSHOT_COMPACT

    @Test
    fun `constant is recognised as compact`() {
        assertTrue(SnapshotCodec.isCompact(constant))
    }

    @Test
    fun `constant decodes to valid json`() {
        val json = SnapshotCodec.normalizeToJson(constant)
        // Throws if not parseable.
        JSONObject(json)
    }

    @Test
    fun `decoded json carries the expected schema`() {
        val obj = JSONObject(SnapshotCodec.normalizeToJson(constant))

        assertTrue(obj.has("version"))
        assertTrue(obj.getInt("version") >= 1)

        val blockHash = obj.getString("block_hash")
        assertEquals(64, blockHash.length)
        assertTrue(blockHash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })

        assertTrue(obj.getLong("height") >= 0L)
        assertTrue(obj.getLong("leaves") >= 0L)
        assertTrue(obj.has("roots"))
        // Must be a JSONArray; throws otherwise.
        obj.getJSONArray("roots")
    }

    @Test
    fun `bundled snapshot is for bitcoin mainnet`() {
        val obj = JSONObject(SnapshotCodec.normalizeToJson(constant))
        assertEquals("bitcoin", obj.getString("network"))
    }

    @Test
    fun `re-encoding the decoded json reproduces the constant`() {
        val json = SnapshotCodec.normalizeToJson(constant)
        val reEncoded = SnapshotCodec.encodeCompact(json)
        assertEquals(constant, reEncoded)
    }
}
