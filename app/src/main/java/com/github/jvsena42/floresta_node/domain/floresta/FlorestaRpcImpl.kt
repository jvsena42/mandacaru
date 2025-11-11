package com.github.jvsena42.floresta_node.domain.floresta

import android.util.Log
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.RpcMethods
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
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

    override suspend fun rescan(): Flow<Result<JSONObject>> = flow {
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"

        Log.d(TAG, "rescan: host:$host")
        val arguments = JSONArray()
        arguments.put(0)

        emit(
            sendJsonRpcRequest(
                host,
                RpcMethods.RESCAN.method,
                arguments
            )
        )
    }

    override suspend fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>> = flow {
        Log.d(TAG, "loadDescriptor: $descriptor")
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"
        val arguments = JSONArray()
        arguments.put(descriptor)

        emit(
            sendJsonRpcRequest(
                host,
                RpcMethods.LOAD_DESCRIPTOR.method,
                arguments
            )
        )
    }

    override suspend fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>> =
        flow {
            Log.d(TAG, "getPeerInfo: ")
            val port = preferencesDataSource.getString(
                key = PreferenceKeys.CURRENT_RPC_PORT,
                defaultValue = Constants.RPC_PORT_MAINNET
            )
            val host = "http://127.0.0.1:$port"
            val arguments = JSONArray()

            sendJsonRpcRequest(
                host,
                RpcMethods.GET_PEER_INFO.method,
                arguments
            ).fold(
                onSuccess = { json ->
                    Log.d(TAG, "getPeerInfo: ")
                    emit(
                        Result.success(
                            gson.fromJson(
                                json.toString(),
                                GetPeerInfoResponse::class.java
                            )
                        )
                    )
                },
                onFailure = { e ->
                    Log.d(TAG, "getPeerInfo: failure: ${e.message}")
                    emit(Result.failure(e))
                }
            )
        }

    override suspend fun stop(): Flow<Result<JSONObject>> = flow {
        Log.d(TAG, "stop: ")
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"
        val arguments = JSONArray()

        emit(
            sendJsonRpcRequest(
                host,
                RpcMethods.STOP.method,
                arguments
            )
        )
    }

    override suspend fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>> = flow {
        Log.d(TAG, "getTransaction: $txId")
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"
        val arguments = JSONArray()
        arguments.put(txId)

        sendJsonRpcRequest(
            host,
            RpcMethods.GET_TRANSACTION.method,
            arguments
        ).fold(
            onSuccess = { json ->
                Log.d(TAG, "getTransaction success: $json")
                try {
                    emit(
                        Result.success(
                            gson.fromJson(
                                json.toString(),
                                GetTransactionResponse::class.java
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "getTransaction parse error: ${e.message}")
                    emit(Result.failure(Exception("Failed to parse transaction data: ${e.message}")))
                }
            },
            onFailure = { e ->
                Log.e(TAG, "getTransaction failure: ${e.message}")
                emit(Result.failure(e))
            }
        )
    }

    override suspend fun listDescriptors(): Flow<Result<JSONObject>> = flow {
        Log.d(TAG, "listDescriptors: ")

        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"
        val arguments = JSONArray()
        emit(
            sendJsonRpcRequest(
                host,
                RpcMethods.LIST_DESCRIPTORS.method,
                arguments
            )
        )
    }

    override suspend fun addNode(node: String): Flow<Result<AddNodeResponse>> {
        Log.d(TAG, "addNode: $node")
        val port = preferencesDataSource.getString(
            key = PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = Constants.RPC_PORT_MAINNET
        )
        val host = "http://127.0.0.1:$port"
        val arguments = JSONArray()
        arguments.put(node)

        return flow {
            sendJsonRpcRequest(
                host,
                RpcMethods.ADD_NODE.method,
                arguments
            ).fold(
                onSuccess = { json ->

                    val response = gson.fromJson(
                        json.toString(),
                        AddNodeResponse::class.java
                    )

                    if (response.result?.success == false) {
                        emit(Result.failure(Exception("Failed to add node")))

                    } else {
                        emit(Result.success(response))
                    }

                },
                onFailure = { e ->
                    emit(Result.failure(e))
                }
            )
        }
    }

    override suspend fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>> =
        flow {
            Log.d(TAG, "getBlockchainInfo: ")
            val port = preferencesDataSource.getString(
                key = PreferenceKeys.CURRENT_RPC_PORT,
                defaultValue = Constants.RPC_PORT_MAINNET
            )
            val host = "http://127.0.0.1:$port"
            val arguments = JSONArray()

            sendJsonRpcRequest(
                host,
                RpcMethods.GET_BLOCKCHAIN_INFO.method,
                arguments
            ).fold(
                onSuccess = { json ->
                    emit(
                        Result.success(
                            gson.fromJson(
                                json.toString(),
                                GetBlockchainInfoResponse::class.java
                            )
                        )
                    )
                },
                onFailure = { e ->
                    emit(Result.failure(e))
                }
            )
        }

    private suspend fun sendJsonRpcRequest(
        endpoint: String,
        method: String,
        params: JSONArray,
    ): Result<JSONObject> {
        Log.d(TAG, "sendJsonRpcRequest: ")
        return try {
            val client = OkHttpClient()

            val jsonRpcRequest = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", method)
                put("params", params)
                put("id", 1)
            }.toString()

            Log.d(TAG, "sendJsonRpcRequest: $jsonRpcRequest")

            val requestBody = jsonRpcRequest.toRequestBody("application/json".toMediaTypeOrNull())

            val request = okhttp3.Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            val body = response.body
            val json = JSONObject(body?.string().orEmpty())

            if (json.has("error")) {
                Result.failure(Exception(json.getJSONObject("error").getString("message")))
            } else {
                Result.success(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendJsonRpcRequest error:", e)
            Result.failure(e)
        }
    }

    private companion object {
        private const val TAG = "FlorestaRpcImpl"
    }

}