package com.github.jvsena42.mandacaru.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class GetBlockHashResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
    val result: String
)
