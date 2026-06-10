package com.github.jvsena42.mandacaru.fakes

import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.AddNodeCommand
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockCountResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHashResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHeaderResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.ListDescriptorsResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.SendRawTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.UptimeResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

/**
 * Hand-rolled fake of [FlorestaRpc]. Every call returns an empty flow by
 * default; the handful of methods used by the screens under test expose
 * configurable results and record their arguments. Override more methods in a
 * subclass when a new test needs them.
 */
open class FakeFlorestaRpc : FlorestaRpc {

    var blockchainInfoResults: List<Result<GetBlockchainInfoResponse>> = emptyList()
    var transactionResult: Result<GetTransactionResponse>? = null
    var sendRawTransactionResult: Result<SendRawTransactionResponse> =
        Result.success(SendRawTransactionResponse(id = 1, jsonrpc = "2.0", result = "txid"))

    val getTransactionCalls = mutableListOf<String>()
    val sentRawTransactions = mutableListOf<String>()

    override fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>> =
        blockchainInfoResults.asFlow()

    override fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> = flow {
        getTransactionCalls.add(txId)
        transactionResult?.let { emit(it) }
    }

    override fun sendRawTransaction(txHex: String): Flow<Result<SendRawTransactionResponse>> = flow {
        sentRawTransactions.add(txHex)
        emit(sendRawTransactionResult)
    }

    override fun rescan(): Flow<Result<JSONObject>> = emptyFlow()
    override fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>> = emptyFlow()
    override fun stop(): Flow<Result<JSONObject>> = emptyFlow()
    override fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> = emptyFlow()
    override fun listDescriptors(): Flow<Result<ListDescriptorsResponse>> = emptyFlow()
    override fun addNode(node: String, command: AddNodeCommand): Flow<Result<AddNodeResponse>> =
        emptyFlow()

    override fun getUptime(): Flow<Result<UptimeResponse>> = emptyFlow()
    override fun getBlockHash(height: Int): Flow<Result<GetBlockHashResponse>> = emptyFlow()
    override fun getBlockHeader(blockHash: String): Flow<Result<GetBlockHeaderResponse>> = emptyFlow()
    override fun getBestBlockHash(): Flow<Result<GetBlockHashResponse>> = emptyFlow()
    override fun getBlockCount(): Flow<Result<GetBlockCountResponse>> = emptyFlow()
    override fun disconnectNode(address: String): Flow<Result<JSONObject>> = emptyFlow()
    override fun ping(): Flow<Result<JSONObject>> = emptyFlow()
}
