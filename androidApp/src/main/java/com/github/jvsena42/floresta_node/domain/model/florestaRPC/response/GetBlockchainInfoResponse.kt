package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response


import com.google.gson.annotations.SerializedName

/**
 * @param id The ID of the JSON-RPC response
 * @param jsonrpc The JSON-RPC version
 * @param result The result of the `getblockchaininfo` RPC call
 */
data class GetBlockchainInfoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
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
data class Result(
    @SerializedName("best_block")
    val bestBlock: String,
    @SerializedName("chain")
    val chain: String,
    @SerializedName("difficulty")
    val difficulty: Float,
    @SerializedName("height")
    val height: Int,
    @SerializedName("ibd")
    val ibd: Boolean,
    @SerializedName("latest_block_time")
    val latestBlockTime: Int,
    @SerializedName("latest_work")
    val latestWork: String,
    @SerializedName("leaf_count")
    val leafCount: Int,
    @SerializedName("progress")
    val progress: Float,
    @SerializedName("root_count")
    val rootCount: Int,
    @SerializedName("root_hashes")
    val rootHashes: List<String>,
    @SerializedName("validated")
    val validated: Int
)