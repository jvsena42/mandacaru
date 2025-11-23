package com.github.jvsena42.floresta_node.domain.floresta

import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.RpcMethods
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.floresta_node.platform.platformLog
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class FlorestaRpcImpl(
    private val httpClient: HttpClient,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaRpc {

    private val rpcHost: String
        get() {
            val port = preferencesDataSource.getString(
                key = PreferenceKeys.CURRENT_RPC_PORT,
                defaultValue = Constants.RPC_PORT_MAINNET
            )
            return "http://127.0.0.1:$port"
        }

    override suspend fun rescan(): Flow<Result<JsonObject>> =
        executeRpcCall(RpcMethods.RESCAN, buildJsonArray { add(0) })

    override suspend fun loadDescriptor(descriptor: String): Flow<Result<JsonObject>> =
        executeRpcCall(RpcMethods.LOAD_DESCRIPTOR, buildJsonArray { add(descriptor) })

    override suspend fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> =
        executeTypedRpcCall(RpcMethods.GET_PEER_INFO)

    override suspend fun stop(): Flow<Result<JsonObject>> =
        executeRpcCall(RpcMethods.STOP)

    override suspend fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> =
        executeTypedRpcCall(RpcMethods.GET_TRANSACTION, buildJsonArray { add(txId) })

    override suspend fun listDescriptors(): Flow<Result<JsonObject>> =
        executeRpcCall(RpcMethods.LIST_DESCRIPTORS)

    override suspend fun addNode(node: String): Flow<Result<AddNodeResponse>> = flow {
        platformLog(TAG, "addNode: $node")
        executeTypedRpcCall<AddNodeResponse>(RpcMethods.ADD_NODE, buildJsonArray { add(node) })
            .collect { result ->
                result.fold(
                    onSuccess = { response ->
                        if (response.result?.success == false) {
                            emit(Result.failure(Exception("Failed to add node")))
                        } else {
                            emit(Result.success(response))
                        }
                    },
                    onFailure = { emit(Result.failure(it)) }
                )
            }
    }

    override suspend fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>> =
        executeTypedRpcCall(RpcMethods.GET_BLOCKCHAIN_INFO)

    private inline fun <reified T> executeTypedRpcCall(
        method: RpcMethods,
        params: JsonArray = buildJsonArray {}
    ): Flow<Result<T>> = flow {
        platformLog(TAG, "${method.method}: $params")

        val result = sendJsonRpcRequest(rpcHost, method.method, params)

        result.fold(
            onSuccess = { jsonStr ->
                try {
                    val response = Json.decodeFromString<T>(jsonStr)
                    emit(Result.success(response))
                } catch (e: Exception) {
                    platformLog(TAG, "${method.method} parse error: ${e.message}")
                    emit(Result.failure(Exception("Failed to parse response: ${e.message}")))
                }
            },
            onFailure = { e ->
                platformLog(TAG, "${method.method} failure: ${e.message}")
                emit(Result.failure(e))
            }
        )
    }

    private fun executeRpcCall(
        method: RpcMethods,
        params: JsonArray = buildJsonArray {}
    ): Flow<Result<JsonObject>> = flow {
        platformLog(TAG, "${method.method}: $params")

        val result = sendJsonRpcRequest(rpcHost, method.method, params)

        result.fold(
            onSuccess = { jsonStr ->
                try {
                    val jsonObject = Json.parseToJsonElement(jsonStr).jsonObject
                    emit(Result.success(jsonObject))
                } catch (e: Exception) {
                    platformLog(TAG, "${method.method} parse error: ${e.message}")
                    emit(Result.failure(Exception("Failed to parse response: ${e.message}")))
                }
            },
            onFailure = { e ->
                platformLog(TAG, "${method.method} failure: ${e.message}")
                emit(Result.failure(e))
            }
        )
    }

    private suspend fun sendJsonRpcRequest(
        endpoint: String,
        method: String,
        params: JsonArray,
    ): Result<String> = runCatching {
        val jsonRpcRequest = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", 1)
        }

        platformLog(TAG, "Request: $jsonRpcRequest")

        val response: HttpResponse = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(jsonRpcRequest.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

        if (jsonResponse.containsKey("error")) {
            val errorMessage = jsonResponse["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: "Unknown error"
            throw Exception(errorMessage)
        }

        responseBody
    }.onFailure { e ->
        platformLog(TAG, "RPC request error: ${e.message}")
    }

    private companion object {
        private const val TAG = "FlorestaRpcImpl"
    }
}

/**
 * Creates an HTTP client configured for JSON-RPC
 */
fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }
}
