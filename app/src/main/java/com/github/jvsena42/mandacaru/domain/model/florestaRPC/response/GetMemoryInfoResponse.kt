package com.github.jvsena42.mandacaru.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class GetMemoryInfoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("result")
    val result: MemoryInfoResult
)

data class MemoryInfoResult(
    @SerializedName("locked")
    val locked: MemInfoLocked
)

data class MemInfoLocked(
    @SerializedName("used")
    val used: Long,
    @SerializedName("free")
    val free: Long,
    @SerializedName("total")
    val total: Long,
    @SerializedName("locked")
    val locked: Long,
    @SerializedName("chunks_used")
    val chunksUsed: Long,
    @SerializedName("chunks_free")
    val chunksFree: Long
)
