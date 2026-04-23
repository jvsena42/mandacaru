package com.github.jvsena42.mandacaru.domain.floresta

import com.florestad.Network
import com.florestad.validateUtreexoSnapshotJson
import com.github.jvsena42.mandacaru.presentation.utils.SnapshotCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SnapshotPreview(
    val network: Network,
    val height: Long,
    val blockHash: String,
    val rootCount: Int,
)

class UtreexoSnapshotService(
    private val daemon: FlorestaDaemon,
) {
    suspend fun dump(): Result<String> =
        daemon.dumpUtreexoState().mapCatching { SnapshotCodec.encodeCompact(it) }

    suspend fun validate(payload: String, expectedNetwork: Network) =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = SnapshotCodec.normalizeToJson(payload)
                validateUtreexoSnapshotJson(json, expectedNetwork)
            }
        }

    fun peek(payload: String): Result<SnapshotPreview> = runCatching {
        val obj = JSONObject(SnapshotCodec.normalizeToJson(payload))
        val network = networkTagToEnum(obj.getString(KEY_NETWORK))
            ?: error("Unknown network tag")
        SnapshotPreview(
            network = network,
            height = obj.getLong(KEY_HEIGHT),
            blockHash = obj.getString(KEY_BLOCK_HASH),
            rootCount = obj.getJSONArray(KEY_ROOTS).length(),
        )
    }

    private fun networkTagToEnum(tag: String): Network? = when (tag) {
        "bitcoin" -> Network.BITCOIN
        "signet" -> Network.SIGNET
        "testnet" -> Network.TESTNET
        "testnet4" -> Network.TESTNET4
        "regtest" -> Network.REGTEST
        else -> null
    }

    private companion object {
        const val KEY_NETWORK = "network"
        const val KEY_HEIGHT = "height"
        const val KEY_BLOCK_HASH = "block_hash"
        const val KEY_ROOTS = "roots"
    }
}
