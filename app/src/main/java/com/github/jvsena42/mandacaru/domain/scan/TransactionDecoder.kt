package com.github.jvsena42.mandacaru.domain.scan

import org.bitcoindevkit.Psbt
import org.bitcoindevkit.Transaction
import java.util.Base64

class TransactionDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface TransactionDecoder {
    fun decode(payload: ByteArray, transport: ScanTransport): Result<DecodedTransaction>
}

/**
 * Turns a scanned payload into a broadcastable raw transaction. A PSBT (detected by its magic
 * bytes) is finalized and extracted, exposing its fee; a raw signed transaction is parsed directly
 * (its fee is unknown because the input amounts are not part of the serialization). Failures are
 * surfaced as a [TransactionDecodeException] wrapped in the returned [Result].
 */
class BdkTransactionDecoder : TransactionDecoder {

    override fun decode(payload: ByteArray, transport: ScanTransport): Result<DecodedTransaction> =
        runCatching {
            if (isPsbt(payload)) decodePsbt(payload, transport) else decodeRawTransaction(payload, transport)
        }

    private fun decodePsbt(payload: ByteArray, transport: ScanTransport): DecodedTransaction {
        val psbt = try {
            Psbt(Base64.getEncoder().encodeToString(payload))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw TransactionDecodeException("Couldn't read the scanned PSBT.", e)
        }
        return psbt.use {
            val feeSats = runCatching { it.fee().toLong() }.getOrNull()
            val tx = try {
                it.extractTx()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                throw TransactionDecodeException(
                    "This PSBT isn't finalized yet. Finalize it on your signing device, then scan again.",
                    e,
                )
            }
            tx.use { finalTx -> summarize(finalTx, feeSats, PayloadType.PSBT, transport) }
        }
    }

    private fun decodeRawTransaction(payload: ByteArray, transport: ScanTransport): DecodedTransaction {
        val tx = try {
            Transaction(payload)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw TransactionDecodeException("Couldn't read the scanned transaction.", e)
        }
        return tx.use { summarize(it, feeSats = null, type = PayloadType.TRANSACTION, transport = transport) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun summarize(
        tx: Transaction,
        feeSats: Long?,
        type: PayloadType,
        transport: ScanTransport,
    ): DecodedTransaction {
        val outputs = tx.output()
        val vsize = tx.vsize().toLong()
        return DecodedTransaction(
            rawHex = tx.serialize().toHexString(),
            txid = tx.computeTxid().toString(),
            inputCount = tx.input().size,
            outputCount = outputs.size,
            totalOutSats = outputs.sumOf { it.value.toSat().toLong() },
            feeSats = feeSats,
            feeRateSatVb = feeSats?.takeIf { vsize > 0 }?.let { it.toDouble() / vsize },
            vsize = vsize,
            weight = tx.weight().toLong(),
            payloadType = type,
            transport = transport,
        )
    }

    private fun isPsbt(payload: ByteArray): Boolean =
        payload.size >= PSBT_MAGIC.size && PSBT_MAGIC.indices.all { payload[it] == PSBT_MAGIC[it] }

    private companion object {
        val PSBT_MAGIC = byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte())
    }
}
