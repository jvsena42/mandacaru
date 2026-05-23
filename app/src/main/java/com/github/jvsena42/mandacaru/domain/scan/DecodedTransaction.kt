package com.github.jvsena42.mandacaru.domain.scan

enum class PayloadType { PSBT, TRANSACTION }

enum class ScanTransport { SINGLE, UR, BBQR }

data class DecodedTransaction(
    val rawHex: String,
    val txid: String,
    val inputCount: Int,
    val outputCount: Int,
    val totalOutSats: Long,
    val feeSats: Long?,
    val feeRateSatVb: Double?,
    val vsize: Long,
    val weight: Long,
    val payloadType: PayloadType,
    val transport: ScanTransport,
)
