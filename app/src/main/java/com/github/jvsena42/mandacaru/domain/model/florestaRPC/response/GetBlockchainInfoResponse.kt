package com.github.jvsena42.mandacaru.domain.model.florestaRPC.response


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
 * Bitcoin Core-compatible `getblockchaininfo` fields plus Floresta's Utreexo, compact-filter and
 * rescan extensions.
 *
 * @param bestBlock The best block we have headers for
 * @param chain The Core-style network name: main, test, testnet4, signet or regtest
 * @param difficulty Current network difficulty
 * @param height The height of the best block we have headers for
 * @param ibd Whether we are currently in initial block download
 * @param latestBlockTime The time in which the latest block was mined
 * @param latestWork The accumulated chain work up to the best block
 * @param leafCount The amount of leaves in our current forest state
 * @param rootCount The amount of roots in our current forest state
 * @param rootHashes The hashes of the roots in our current forest state
 * @param validated The amount of blocks we have validated so far
 */
data class Result(
    @SerializedName("bestblockhash")
    val bestBlock: String,
    @SerializedName("chain")
    val chain: String,
    @SerializedName("difficulty")
    val difficulty: Float,
    @SerializedName("headers")
    val height: Int,
    @SerializedName("initialblockdownload")
    val ibd: Boolean,
    @SerializedName("time")
    val latestBlockTime: Int,
    @SerializedName("chainwork")
    val latestWork: String,
    @SerializedName("leaf_count")
    val leafCount: Long,
    @SerializedName("root_count")
    val rootCount: Int,
    @SerializedName("root_hashes")
    val rootHashes: List<String>,
    @SerializedName("blocks")
    val validated: Int,
    @SerializedName("filters")
    val filters: Int? = null,
    @SerializedName("rescan_in_progress")
    val rescanInProgress: Boolean = false,
    @SerializedName("rescan_blocks_processed")
    val rescanBlocksProcessed: Int? = null,
    @SerializedName("rescan_blocks_total")
    val rescanBlocksTotal: Int? = null,
    @SerializedName("rescan_error")
    val rescanError: String? = null,
) {
    /**
     * Fraction of known headers whose blocks are validated. Core's `verificationprogress` is
     * time-based and never reaches exactly 1, so it can't gate "fully synced".
     */
    val progress: Float
        get() = if (height > 0) validated.toFloat() / height else 0f

    val networkName: String
        get() = when (chain) {
            "main" -> "bitcoin"
            "test" -> "testnet"
            else -> chain
        }
}
