package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class GetBlockCountResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
    val result: Int
)
