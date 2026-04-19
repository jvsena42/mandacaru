package com.github.jvsena42.mandacaru.domain.floresta

import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockCountResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHashResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHeaderResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetMemoryInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.ListDescriptorsResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.PeerInfoResult
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.ResultAddNode
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.SendRawTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.UptimeResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtreexoBridgeAutoConnectTest {

    @Test
    fun `ensureUtreexoPeers fires onetry then add for each bridge when no utreexo peer is present`() = runBlocking {
        val rpc = FakeFlorestaRpc(
            peers = listOf(peer("NETWORK|WITNESS|NETWORK_LIMITED|P2P_V2"))
        )
        val prefs = FakePreferences(network = "BITCOIN")
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { 0L })

        sut.ensureUtreexoPeers()

        assertEquals(
            listOf(
                "189.44.63.101:8333" to "onetry",
                "189.44.63.101:8333" to "add",
                "195.26.240.213:8333" to "onetry",
                "195.26.240.213:8333" to "add",
            ),
            rpc.addNodeCalls
        )
    }

    @Test
    fun `ensureUtreexoPeers does nothing when a peer already advertises utreexo`() = runBlocking {
        val rpc = FakeFlorestaRpc(
            peers = listOf(peer("NETWORK|WITNESS|0x1000"))
        )
        val prefs = FakePreferences(network = "BITCOIN")
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { 0L })

        sut.ensureUtreexoPeers()

        assertTrue(rpc.addNodeCalls.isEmpty())
    }

    @Test
    fun `ensureUtreexoPeers does nothing when no bridges are configured for the network`() = runBlocking {
        val rpc = FakeFlorestaRpc(peers = emptyList())
        val prefs = FakePreferences(network = "REGTEST")
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { 0L })

        sut.ensureUtreexoPeers()

        assertTrue(rpc.addNodeCalls.isEmpty())
        // Also skipped the peer lookup entirely.
        assertEquals(0, rpc.getPeerInfoCallCount)
    }

    @Test
    fun `ensureUtreexoPeers honours the 60 second throttle`() = runBlocking {
        val rpc = FakeFlorestaRpc(peers = emptyList())
        val prefs = FakePreferences(network = "BITCOIN")
        var now = 0L
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { now })

        sut.ensureUtreexoPeers()
        val firstCallSize = rpc.addNodeCalls.size

        now = 30_000L
        sut.ensureUtreexoPeers()
        assertEquals(
            "Within throttle window, addNode must not be called again",
            firstCallSize,
            rpc.addNodeCalls.size
        )

        now = 61_000L
        sut.ensureUtreexoPeers()
        assertEquals(
            "After throttle window, addNode is re-fired",
            firstCallSize * 2,
            rpc.addNodeCalls.size
        )
    }

    @Test
    fun `seedOnStartup fires immediately even before the throttle window`() = runBlocking {
        val rpc = FakeFlorestaRpc(peers = emptyList())
        val prefs = FakePreferences(network = "BITCOIN")
        var now = 1_000L
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { now })

        sut.seedOnStartup()
        val afterStartup = rpc.addNodeCalls.size
        // 2 bridges x 2 commands (onetry + add) = 4
        assertEquals(4, afterStartup)

        // A follow-up ensureUtreexoPeers within 60s should be throttled.
        now = 10_000L
        sut.ensureUtreexoPeers()
        assertEquals(afterStartup, rpc.addNodeCalls.size)
    }

    @Test
    fun `signet network uses the single known bridge`() = runBlocking {
        val rpc = FakeFlorestaRpc(peers = emptyList())
        val prefs = FakePreferences(network = "SIGNET")
        val sut = UtreexoBridgeAutoConnect(rpc, prefs, nowMs = { 0L })

        sut.ensureUtreexoPeers()

        assertEquals(
            listOf(
                "189.44.63.101:38333" to "onetry",
                "189.44.63.101:38333" to "add",
            ),
            rpc.addNodeCalls
        )
    }

    private fun peer(services: String) = PeerInfoResult(
        address = "1.2.3.4:8333",
        initialHeight = 0,
        kind = "regular",
        services = "ServiceFlags($services)",
        state = "Ready",
        userAgent = "/test/"
    )
}

private class FakePreferences(private val network: String) : PreferencesDataSource {
    override suspend fun setString(key: PreferenceKeys, value: String) = Unit
    override suspend fun getString(key: PreferenceKeys, defaultValue: String): String =
        if (key == PreferenceKeys.CURRENT_NETWORK) network else defaultValue
    override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) = Unit
    override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean): Boolean = defaultValue
}

private class FakeFlorestaRpc(
    private val peers: List<PeerInfoResult>
) : FlorestaRpc {
    val addNodeCalls = mutableListOf<Pair<String, String>>()
    var getPeerInfoCallCount = 0
        private set

    override fun addNode(node: String, command: String): Flow<Result<AddNodeResponse>> {
        addNodeCalls.add(node to command)
        return flowOf(Result.success(AddNodeResponse(id = 1, jsonrpc = "2.0", result = ResultAddNode(success = true))))
    }

    override fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> {
        getPeerInfoCallCount++
        return flowOf(Result.success(GetPeerInfoResponse(id = 1, jsonrpc = "2.0", result = peers)))
    }

    // Unused methods — fail loudly if the helper starts depending on them.
    override fun rescan(): Flow<Result<JSONObject>> = TODO("not used")
    override fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>> = TODO("not used")
    override fun stop(): Flow<Result<JSONObject>> = TODO("not used")
    override fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> = TODO("not used")
    override fun listDescriptors(): Flow<Result<ListDescriptorsResponse>> = TODO("not used")
    override fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>> = TODO("not used")
    override fun getUptime(): Flow<Result<UptimeResponse>> = TODO("not used")
    override fun getMemoryInfo(): Flow<Result<GetMemoryInfoResponse>> = TODO("not used")
    override fun getBlockHash(height: Int): Flow<Result<GetBlockHashResponse>> = TODO("not used")
    override fun getBlockHeader(blockHash: String): Flow<Result<GetBlockHeaderResponse>> = TODO("not used")
    override fun getBestBlockHash(): Flow<Result<GetBlockHashResponse>> = TODO("not used")
    override fun getBlockCount(): Flow<Result<GetBlockCountResponse>> = TODO("not used")
    override fun sendRawTransaction(txHex: String): Flow<Result<SendRawTransactionResponse>> = TODO("not used")
    override fun disconnectNode(address: String): Flow<Result<JSONObject>> = TODO("not used")
    override fun ping(): Flow<Result<JSONObject>> = TODO("not used")
}
