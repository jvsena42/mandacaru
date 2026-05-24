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
 * (its fee is unknown because the input amounts are not part of the serialization).
 *
 * Transactions that aren't fully signed are rejected here, because the node can't catch them: a
 * utreexo node keeps no UTXO set, so its `sendrawtransaction` only runs context-free checks and
 * returns a txid even for an unsigned transaction. Failures are surfaced as a
 * [TransactionDecodeException] wrapped in the returned [Result].
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
            finalizeAndExtract(it).use { finalTx ->
                ensureFullySigned(finalTx)
                summarize(finalTx, feeSats, PayloadType.PSBT, transport)
            }
        }
    }

    private fun finalizeAndExtract(psbt: Psbt): Transaction {
        val finalized = psbt.finalize()
        if (!finalized.couldFinalize) {
            throw TransactionDecodeException(
                "This PSBT isn't fully signed yet. Sign it on your signing device, then scan again.",
            )
        }
        return finalized.psbt.use { finalPsbt ->
            try {
                finalPsbt.extractTx()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                throw TransactionDecodeException("Couldn't extract the transaction from this PSBT.", e)
            }
        }
    }

    private fun decodeRawTransaction(payload: ByteArray, transport: ScanTransport): DecodedTransaction {
        val tx = try {
            Transaction(payload)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw TransactionDecodeException("Couldn't read the scanned transaction.", e)
        }
        return tx.use {
            ensureFullySigned(it)
            summarize(it, feeSats = null, type = PayloadType.TRANSACTION, transport = transport)
        }
    }

    private fun ensureFullySigned(tx: Transaction) {
        val hasUnsignedInput = tx.input().any { it.scriptSig.toBytes().isEmpty() && it.witness.isEmpty() }
        if (hasUnsignedInput) {
            throw TransactionDecodeException(
                "This transaction isn't fully signed, so it can't be broadcast. " +
                    "Sign it on your signing device, then scan again.",
            )
        }
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
