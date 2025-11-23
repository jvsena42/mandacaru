package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class AddNodeResponse(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("jsonrpc")
    val jsonrpc: String?,
    @SerializedName("result")
    val result: ResultAddNode?
)

data class ResultAddNode(
    @SerializedName("success")
    val success: Boolean
)