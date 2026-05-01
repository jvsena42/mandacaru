package com.github.jvsena42.mandacaru.data.floresta

import android.util.Log
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.Constants
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.AddNodeCommand
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.RpcMethods
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockCountResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHashResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHeaderResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetMemoryInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.SendRawTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.ListDescriptorsResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.UptimeResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FlorestaRpcImpl(
    private val gson: Gson,
    private val preferencesDataSource: PreferencesDataSource
) : FlorestaRpc {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun getRpcHost(): String {
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        return "http://127.0.0.1:$port"
    }

    override fun rescan(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.RESCAN, params = arrayOf(0))

    override fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.LOAD_DESCRIPTOR, params = arrayOf(descriptor))

    override fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> =
        executeRpcCall(RpcMethods.GET_PEER_INFO)

    override fun stop(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.STOP)

    override fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> =
        executeRpcCall(RpcMethods.GET_TRANSACTION, params = arrayOf(txId))

    override fun listDescriptors(): Flow<Result<ListDescriptorsResponse>> =
        executeRpcCall(RpcMethods.LIST_DESCRIPTORS)

    override fun addNode(node: String, command: AddNodeCommand): Flow<Result<AddNodeResponse>> {
        Log.d(TAG, "addNode: $node (${command.value})")
        return executeRpcCall<AddNodeResponse>(RpcMethods.ADD_NODE, params = arrayOf(node, command.value))
            .map { result ->
                result.fold(
                    onSuccess = { response ->
                        if (response.result?.success == false) {
                            Result.failure(Exception("Failed to add node"))
                        } else {
                            Result.success(response)
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            }
    }

    override fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>> =
        executeRpcCall(RpcMethods.GET_BLOCKCHAIN_INFO)

    override fun getUptime(): Flow<Result<UptimeResponse>> =
        executeRpcCall(RpcMethods.UPTIME)

    override fun getMemoryInfo(): Flow<Result<GetMemoryInfoResponse>> =
        executeRpcCall(RpcMethods.GET_MEMORY_INFO, "stats")

    override fun getBlockHash(height: Int): Flow<Result<GetBlockHashResponse>> =
        executeRpcCall(RpcMethods.GET_BLOCK_HASH, height)

    override fun getBlockHeader(blockHash: String): Flow<Result<GetBlockHeaderResponse>> =
        executeRpcCall(RpcMethods.GET_BLOCK_HEADER, blockHash)

    override fun getBestBlockHash(): Flow<Result<GetBlockHashResponse>> =
        executeRpcCall(RpcMethods.GET_BEST_BLOCK_HASH)

    override fun getBlockCount(): Flow<Result<GetBlockCountResponse>> =
        executeRpcCall(RpcMethods.GET_BLOCK_COUNT)

    override fun sendRawTransaction(txHex: String): Flow<Result<SendRawTransactionResponse>> =
        executeRpcCall(RpcMethods.SEND_RAW_TRANSACTION, txHex)

    override fun disconnectNode(address: String): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.DISCONNECT_NODE, address)

    override fun ping(): Flow<Result<JSONObject>> =
        executeRpcCall(RpcMethods.PING)

    private inline fun <reified T> executeRpcCall(
        method: RpcMethods,
        vararg params: Any
    ): Flow<Result<T>> = flow {
        Log.d(TAG, "${method.method}: ${params.joinToString()}")

        val result = withContext(Dispatchers.IO) {
            sendJsonRpcRequest(getRpcHost(), method.method, params.toJsonArray())
        }

        val emission = result.fold(
            onSuccess = { json ->
                try {
                    val response = when (T::class) {
                        JSONObject::class -> json as T
                        else -> gson.fromJson(json.toString(), T::class.java)
                    }
                    Result.success(response)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Log.e(TAG, "${method.method} parse error: ${e.message}")
                    Result.failure(Exception("Failed to parse response: ${e.message}"))
                }
            },
            onFailure = { e ->
                Log.e(TAG, "${method.method} failure: ${e.message}")
                Result.failure(e)
            }
        )
        emit(emission)
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
            val body = response.body.string()
            Log.d(TAG, "Response ($method): $body")
            val json = JSONObject(body)

            if (json.has("error")) {
                @Suppress("TooGenericExceptionThrown")
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
