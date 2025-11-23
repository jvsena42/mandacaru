package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddNodeResponse(
    @SerialName("id")
    val id: Int?,
    @SerialName("jsonrpc")
    val jsonrpc: String?,
    @SerialName("result")
    val result: ResultAddNode?
)

@Serializable
data class ResultAddNode(
    @SerialName("success")
    val success: Boolean
)
