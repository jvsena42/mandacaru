package com.github.jvsena42.mandacaru.domain.model.florestaRPC.response

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
    @SerializedName("previousblockhash")
    val prevBlockhash: String? = null,
    @SerializedName("merkleroot")
    val merkleRoot: String,
    @SerializedName("time")
    val time: Long,
    @SerializedName("bits")
    val bits: String,
    @SerializedName("nonce")
    val nonce: Long
)
