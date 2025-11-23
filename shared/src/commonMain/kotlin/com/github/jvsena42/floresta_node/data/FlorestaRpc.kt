package com.github.jvsena42.floresta_node.data

import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface FlorestaRpc {
    suspend fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>>
    suspend fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>>
    suspend fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>>
    suspend fun addNode(node: String): Flow<Result<AddNodeResponse>>
    suspend fun loadDescriptor(descriptor: String): Flow<Result<JsonObject>>
    suspend fun rescan(): Flow<Result<JsonObject>>
    suspend fun listDescriptors(): Flow<Result<JsonObject>>
    suspend fun stop(): Flow<Result<JsonObject>>
}
