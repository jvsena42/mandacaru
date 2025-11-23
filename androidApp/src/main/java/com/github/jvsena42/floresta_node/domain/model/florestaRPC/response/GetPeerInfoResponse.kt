package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response


import com.google.gson.annotations.SerializedName

data class GetPeerInfoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
    val result: List<PeerInfoResult>?
)

data class PeerInfoResult(
    @SerializedName("address")
    val address: String,
    @SerializedName("initial_height")
    val initialHeight: Int,
    @SerializedName("kind")
    val kind: String,
    @SerializedName("services")
    val services: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("user_agent")
    val userAgent: String
)