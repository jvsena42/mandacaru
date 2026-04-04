package com.github.jvsena42.floresta_node.domain.floresta

import android.util.Log
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.RpcMethods
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetMemoryInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.UptimeResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class FlorestaRpcImpl(
    private val gson: Gson,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaRpc {

    private val client by lazy { OkHttpClient() }

    private val rpcHost: String
        get() {
            val port = preferencesDataSource.getString(
                key = PreferenceKeys.CURRENT_RPC_PORT,
                defaultValue = Constants.RPC_PORT_MAINNET
            )
            return "http://127.0.0.1:$port"
        }

    override suspend fun rescan(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.RESCAN, params = arrayOf(0))

    override suspend fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.LOAD_DESCRIPTOR, params = arrayOf(descriptor))

    override suspend fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> =
        executeRpcCall(RpcMethods.GET_PEER_INFO)

    override suspend fun stop(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.STOP)

    override suspend fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> =
        executeRpcCall(RpcMethods.GET_TRANSACTION, params = arrayOf(txId))

    override suspend fun listDescriptors(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.LIST_DESCRIPTORS)

    override suspend fun addNode(node: String): Flow<Result<AddNodeResponse>> = flow {
        Log.d(TAG, "addNode: $node")
        executeRpcCall<AddNodeResponse>(RpcMethods.ADD_NODE, params = arrayOf(node))
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
        executeRpcCall(RpcMethods.GET_BLOCKCHAIN_INFO)

    override suspend fun getUptime(): Flow<Result<UptimeResponse>> =
        executeRpcCall(RpcMethods.UPTIME)

    override suspend fun getMemoryInfo(): Flow<Result<GetMemoryInfoResponse>> =
        executeRpcCall(RpcMethods.GET_MEMORY_INFO, "stats")

    private inline fun <reified T> executeRpcCall(
        method: RpcMethods,
        vararg params: Any
    ): Flow<Result<T>> = flow {
        Log.d(TAG, "${method.method}: ${params.joinToString()}")

        val result = sendJsonRpcRequest(rpcHost, method.method, params.toJsonArray())

        result.fold(
            onSuccess = { json ->
                try {
                    val response = when (T::class) {
                        JSONObject::class -> json as T
                        else -> gson.fromJson(json.toString(), T::class.java)
                    }
                    emit(Result.success(response))
                } catch (e: Exception) {
                    Log.e(TAG, "${method.method} parse error: ${e.message}")
                    emit(Result.failure(Exception("Failed to parse response: ${e.message}")))
                }
            },
            onFailure = { e ->
                Log.e(TAG, "${method.method} failure: ${e.message}")
                emit(Result.failure(e))
            }
        )
    }

    private fun Array<out Any>.toJsonArray() = JSONArray().apply {
        forEach { put(it) }
    }

    private fun sendJsonRpcRequest(
        endpoint: String,
        method: String,
        params: JSONArray,
    ): Result<JSONObject> = runCatching {
        val jsonRpcRequest = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", 1)
        }.toString()

        Log.d(TAG, "Request: $jsonRpcRequest")

        val requestBody = jsonRpcRequest.toRequestBody("application/json".toMediaTypeOrNull())
        val request = okhttp3.Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val json = JSONObject(response.body.string())

            if (json.has("error")) {
                throw Exception(json.getJSONObject("error").getString("message"))
            }
            json
        }
    }.onFailure { e ->
        Log.e(TAG, "RPC request error:", e)
    }

    private companion object {
        private const val TAG = "FlorestaRpcImpl"
    }

}