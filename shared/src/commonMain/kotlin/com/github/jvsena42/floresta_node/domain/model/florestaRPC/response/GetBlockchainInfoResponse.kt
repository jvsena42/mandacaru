package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param id The ID of the JSON-RPC response
 * @param jsonrpc The JSON-RPC version
 * @param result The result of the `getblockchaininfo` RPC call
 */
@Serializable
data class GetBlockchainInfoResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("result")
    val result: Result
)

/**
 * @param bestBlock The best block we have headers for
 * @param chain The name of the current active network (e.g., bitcoin, testnet, regtest)
 * @param difficulty Current network difficulty
 * @param height The height of the best block we have headers for
 * @param ibd Whether we are currently in initial block download
 * @param latestBlockTime The time in which the latest block was mined
 * @param latestWork The work of the latest block (e.g., the amount of hashes needed to mine it, on average)
 * @param leafCount The amount of leaves in our current forest state
 * @param progress The percentage of blocks we have validated so far
 * @param rootCount The amount of roots in our current forest state
 * @param rootHashes The hashes of the roots in our current forest state
 * @param validated The amount of blocks we have validated so far
 */
@Serializable
data class Result(
    @SerialName("best_block")
    val bestBlock: String,
    @SerialName("chain")
    val chain: String,
    @SerialName("difficulty")
    val difficulty: Float,
    @SerialName("height")
    val height: Int,
    @SerialName("ibd")
    val ibd: Boolean,
    @SerialName("latest_block_time")
    val latestBlockTime: Int,
    @SerialName("latest_work")
    val latestWork: String,
    @SerialName("leaf_count")
    val leafCount: Int,
    @SerialName("progress")
    val progress: Float,
    @SerialName("root_count")
    val rootCount: Int,
    @SerialName("root_hashes")
    val rootHashes: List<String>,
    @SerialName("validated")
    val validated: Int
)
