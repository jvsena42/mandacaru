package com.github.jvsena42.mandacaru.domain.scan

import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.sparrowwallet.hummingbird.registry.CryptoAccount
import com.sparrowwallet.hummingbird.registry.CryptoOutput
import com.sparrowwallet.hummingbird.registry.URAccountDescriptor
import com.sparrowwallet.hummingbird.registry.UROutputDescriptor

sealed interface DescriptorScanState {
    data object Idle : DescriptorScanState

    data class InProgress(val progress: Float) : DescriptorScanState

    data class Complete(val descriptor: String) : DescriptorScanState

    data class Error(val reason: String) : DescriptorScanState
}

interface DescriptorQrScanner {
    fun ingest(raw: String): DescriptorScanState
    fun reset()
}

/**
 * Accumulates scanned QR strings into a single wallet descriptor. Handles single-frame raw
 * descriptors, JSON wallet exports and multisig "setup files" (via [DescriptorUtils.extractDescriptor]),
 * animated UR/BCUR (hummingbird `ur:bytes` / `ur:output-descriptor` / `ur:account-descriptor`) and
 * animated BBQr. The transport is locked in by the first frame until [reset] or completion.
 */
class DefaultDescriptorQrScanner : DescriptorQrScanner {

    private var urDecoder: URDecoder? = null
    private var bbqrJoiner: BbqrJoiner? = null

    override fun ingest(raw: String): DescriptorScanState {
        val text = raw.trim()
        if (text.isEmpty()) return DescriptorScanState.Idle
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

    private fun ingestUr(part: String): DescriptorScanState {
        val decoder = urDecoder ?: URDecoder().also { urDecoder = it }
        decoder.receivePart(part)
        val result = decoder.result ?: return DescriptorScanState.InProgress(
            decoder.estimatedPercentComplete.toFloat().coerceIn(0f, PROGRESS_CEILING)
        )
        return when (result.type) {
            ResultType.SUCCESS -> decodeUr(result.ur)
            else -> failWith(result.error ?: "Failed to decode the animated QR code")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun decodeUr(ur: UR): DescriptorScanState =
        try {
            when (val decoded = ur.decodeFromRegistry()) {
                is UROutputDescriptor -> completeWith(decoded.source)
                is URAccountDescriptor ->
                    completeWith(decoded.outputDescriptors.firstOrNull()?.source)
                is ByteArray -> completeWith(DescriptorUtils.extractDescriptor(String(decoded)))
                is CryptoOutput, is CryptoAccount -> failWith(LEGACY_UR_ERROR)
                else -> failWith("This QR code doesn't contain a wallet descriptor")
            }
        } catch (e: Exception) {
            failWith(e.message ?: "Failed to decode the wallet descriptor")
        }

    private fun ingestBbqr(part: String): DescriptorScanState {
        val joiner = bbqrJoiner ?: BbqrJoiner().also { bbqrJoiner = it }
        return try {
            when (val result = joiner.addPart(part)) {
                is BbqrJoiner.Result.InProgress -> {
                    val progress = result.received.toFloat() / result.total
                    DescriptorScanState.InProgress(progress.coerceIn(0f, PROGRESS_CEILING))
                }
                is BbqrJoiner.Result.Complete ->
                    completeWith(DescriptorUtils.extractDescriptor(String(result.data)))
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            failWith(e.message ?: "Failed to decode the BBQr code")
        }
    }

    private fun ingestSingleFrame(text: String): DescriptorScanState =
        completeWith(DescriptorUtils.extractDescriptor(text))

    private fun completeWith(descriptor: String?): DescriptorScanState {
        if (descriptor.isNullOrBlank()) return failWith("This QR code doesn't contain a wallet descriptor")
        reset()
        return DescriptorScanState.Complete(descriptor)
    }

    private fun failWith(reason: String): DescriptorScanState {
        reset()
        return DescriptorScanState.Error(reason)
    }

    private companion object {
        const val UR_PREFIX = "ur:"
        const val BBQR_PREFIX = "B$"
        const val PROGRESS_CEILING = 0.99f
        const val LEGACY_UR_ERROR =
            "This UR type isn't supported yet — on your signer choose " +
                "'output descriptor' / 'coordination setup', or paste the descriptor"
    }
}
