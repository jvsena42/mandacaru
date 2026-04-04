package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class GetBlockHeaderResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
    val result: BlockHeaderResult
)

data class BlockHeaderResult(
    @SerializedName("version")
    val version: Int,
    @SerializedName("prev_blockhash")
    val prevBlockhash: String,
    @SerializedName("merkle_root")
    val merkleRoot: String,
    @SerializedName("time")
    val time: Long,
    @SerializedName("bits")
    val bits: Long,
    @SerializedName("nonce")
    val nonce: Long
)
