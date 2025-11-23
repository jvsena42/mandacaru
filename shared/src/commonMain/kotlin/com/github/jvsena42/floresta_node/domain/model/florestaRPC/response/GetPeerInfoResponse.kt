package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPeerInfoResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("result")
    val result: List<PeerInfoResult>?
)

@Serializable
data class PeerInfoResult(
    @SerialName("address")
    val address: String,
    @SerialName("initial_height")
    val initialHeight: Int,
    @SerialName("kind")
    val kind: String,
    @SerialName("services")
    val services: String,
    @SerialName("state")
    val state: String,
    @SerialName("user_agent")
    val userAgent: String
)
