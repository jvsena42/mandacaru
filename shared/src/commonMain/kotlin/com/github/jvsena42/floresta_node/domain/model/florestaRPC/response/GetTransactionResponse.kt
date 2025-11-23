package com.github.jvsena42.floresta_node.domain.model.florestaRPC.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTransactionResponse(
    @SerialName("id")
    val id: Int?,
    @SerialName("jsonrpc")
    val jsonrpc: String?,
    @SerialName("result")
    val result: TransactionResult?
)

@Serializable
data class TransactionResult(
    @SerialName("blockhash")
    val blockhash: String?,
    @SerialName("blocktime")
    val blocktime: Long?,
    @SerialName("confirmations")
    val confirmations: Int?,
    @SerialName("hash")
    val hash: String?,
    @SerialName("hex")
    val hex: String?,
    @SerialName("in_active_chain")
    val inActiveChain: Boolean?,
    @SerialName("locktime")
    val locktime: Long?,
    @SerialName("size")
    val size: Int?,
    @SerialName("time")
    val time: Long?,
    @SerialName("txid")
    val txid: String?,
    @SerialName("version")
    val version: Int?,
    @SerialName("vin")
    val vin: List<TransactionInput>?,
    @SerialName("vout")
    val vout: List<TransactionOutput>?,
    @SerialName("vsize")
    val vsize: Int?,
    @SerialName("weight")
    val weight: Int?
)

@Serializable
data class TransactionInput(
    @SerialName("txid")
    val txid: String?,
    @SerialName("vout")
    val vout: Int?,
    @SerialName("scriptSig")
    val scriptSig: ScriptSig?,
    @SerialName("sequence")
    val sequence: Long?,
    @SerialName("txinwitness")
    val txinwitness: List<String>?
)

@Serializable
data class ScriptSig(
    @SerialName("asm")
    val asm: String?,
    @SerialName("hex")
    val hex: String?
)

@Serializable
data class TransactionOutput(
    @SerialName("value")
    val value: Double?,
    @SerialName("n")
    val n: Int?,
    @SerialName("scriptPubKey")
    val scriptPubKey: ScriptPubKey?
)

@Serializable
data class ScriptPubKey(
    @SerialName("asm")
    val asm: String?,
    @SerialName("hex")
    val hex: String?,
    @SerialName("reqSigs")
    val reqSigs: Int?,
    @SerialName("type")
    val type: String?,
    @SerialName("addresses")
    val addresses: List<String>?
)
