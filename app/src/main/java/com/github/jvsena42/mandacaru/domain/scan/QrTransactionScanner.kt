package com.github.jvsena42.mandacaru.domain.scan

import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import java.util.Base64

interface QrTransactionScanner {
    fun ingest(raw: String): ScanState
    fun reset()
}

/**
 * Accumulates scanned QR strings into a single signed payload. Handles single-frame raw hex /
 * base64 / PSBT, animated UR/BCUR (hummingbird) and animated BBQr (bbqr-android). The transport is
 * locked in by the first frame and stays until [reset] or completion.
 */
class DefaultQrTransactionScanner : QrTransactionScanner {

    private var urDecoder: URDecoder? = null
    private var bbqrJoiner: BbqrJoiner? = null

    override fun ingest(raw: String): ScanState {
        val text = raw.trim()
        if (text.isEmpty()) return ScanState.Idle
        return when {
            text.startsWith(UR_PREFIX, ignoreCase = true) -> ingestUr(text)
            text.startsWith(BBQR_PREFIX) -> ingestBbqr(text)
            else -> ingestSingleFrame(text)
        }
    }

    override fun reset() {
        urDecoder = null
        bbqrJoiner = null
    }

    private fun ingestUr(part: String): ScanState {
        val decoder = urDecoder ?: URDecoder().also { urDecoder = it }
        decoder.receivePart(part)
        val result = decoder.result ?: return ScanState.InProgress(
            decoder.estimatedPercentComplete.toFloat().coerceIn(0f, PROGRESS_CEILING)
        )
        return when (result.type) {
            ResultType.SUCCESS -> completeWith(result.ur, ScanTransport.UR)
            else -> failWith(result.error ?: "Failed to decode the animated QR code")
        }
    }

    private fun completeWith(ur: UR, transport: ScanTransport): ScanState =
        runCatching {
            ScanState.Complete(ur.toBytes(), transport).also { reset() }
        }.getOrElse { failWith(it.message ?: "Unsupported QR payload") }

    private fun ingestBbqr(part: String): ScanState {
        val joiner = bbqrJoiner ?: BbqrJoiner().also { bbqrJoiner = it }
        return runCatching {
            when (val result = joiner.addPart(part)) {
                is BbqrJoiner.Result.InProgress -> {
                    val progress = result.received.toFloat() / result.total
                    ScanState.InProgress(progress.coerceIn(0f, PROGRESS_CEILING))
                }
                is BbqrJoiner.Result.Complete ->
                    ScanState.Complete(result.data, ScanTransport.BBQR).also { reset() }
            }
        }.getOrElse { failWith(it.message ?: "Failed to decode the BBQr code") }
    }

    private fun ingestSingleFrame(text: String): ScanState {
        val bytes = decodeSingleFrame(text)
            ?: return failWith("Unrecognized QR code content")
        return ScanState.Complete(bytes, ScanTransport.SINGLE).also { reset() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decodeSingleFrame(text: String): ByteArray? {
        val candidate = text.removePrefix(HEX_PREFIX)
        if (candidate.length % 2 == 0 && candidate.matches(HEX_REGEX)) {
            return candidate.hexToByteArray()
        }
        return runCatching { Base64.getDecoder().decode(text) }.getOrNull()
    }

    private fun failWith(reason: String): ScanState {
        reset()
        return ScanState.Error(reason)
    }

    private companion object {
        const val UR_PREFIX = "ur:"
        const val BBQR_PREFIX = "B$"
        const val HEX_PREFIX = "0x"
        const val PROGRESS_CEILING = 0.99f
        val HEX_REGEX = Regex("^[0-9a-fA-F]+$")
    }
}
