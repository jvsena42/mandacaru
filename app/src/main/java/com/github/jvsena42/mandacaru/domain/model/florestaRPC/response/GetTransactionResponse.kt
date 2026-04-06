package com.github.jvsena42.mandacaru.domain.model.florestaRPC.response

import com.google.gson.annotations.SerializedName

data class GetTransactionResponse(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("jsonrpc")
    val jsonrpc: String?,
    @SerializedName("result")
    val result: TransactionResult?
)

data class TransactionResult(
    @SerializedName("blockhash")
    val blockhash: String?,
    @SerializedName("blocktime")
    val blocktime: Long?,
    @SerializedName("confirmations")
    val confirmations: Int?,
    @SerializedName("hash")
    val hash: String?,
    @SerializedName("hex")
    val hex: String?,
    @SerializedName("in_active_chain")
    val inActiveChain: Boolean?,
    @SerializedName("locktime")
    val locktime: Long?,
    @SerializedName("size")
    val size: Int?,
    @SerializedName("time")
    val time: Long?,
    @SerializedName("txid")
    val txid: String?,
    @SerializedName("version")
    val version: Int?,
    @SerializedName("vin")
    val vin: List<TransactionInput>?,
    @SerializedName("vout")
    val vout: List<TransactionOutput>?,
    @SerializedName("vsize")
    val vsize: Int?,
    @SerializedName("weight")
    val weight: Int?
)

data class TransactionInput(
    @SerializedName("txid")
    val txid: String?,
    @SerializedName("vout")
    val vout: Int?,
    @SerializedName("scriptSig")
    val scriptSig: ScriptSig?,
    @SerializedName("sequence")
    val sequence: Long?,
    @SerializedName("txinwitness")
    val txinwitness: List<String>?
)

data class ScriptSig(
    @SerializedName("asm")
    val asm: String?,
    @SerializedName("hex")
    val hex: String?
)

data class TransactionOutput(
    @SerializedName("value")
    val value: Double?,
    @SerializedName("n")
    val n: Int?,
    @SerializedName("scriptPubKey")
    val scriptPubKey: ScriptPubKey?
)

data class ScriptPubKey(
    @SerializedName("asm")
    val asm: String?,
    @SerializedName("hex")
    val hex: String?,
    @SerializedName("reqSigs")
    val reqSigs: Int?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("addresses")
    val addresses: List<String>?
)
