package com.github.jvsena42.mandacaru.domain.scan

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test: [BdkTransactionDecoder] parses transactions through BDK's
 * native (`bdkffi`) library, so it must run on a device/emulator. The critical
 * guard under test is [BdkTransactionDecoder] rejecting transactions that are
 * not fully signed — the utreexo node can't catch those (it keeps no UTXO set
 * and returns a txid even for unsigned transactions).
 */
@RunWith(AndroidJUnit4::class)
class BdkTransactionDecoderTest {

    private val decoder = BdkTransactionDecoder()

    @Test
    fun unsignedTransaction_isRejected() {
        // One input with an empty scriptSig and no witness — i.e. not signed.
        val zero32 = "00".repeat(32)
        val zero20 = "00".repeat(20)
        val unsignedHex = "02000000" + "01" + zero32 + "00000000" + "00" + "ffffffff" +
            "01" + "00e1f50500000000" + "19" + "76a914" + zero20 + "88ac" + "00000000"

        val result = decoder.decode(unsignedHex.hexToBytes(), ScanTransport.SINGLE)

        assertTrue("Unsigned tx must be rejected", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is TransactionDecodeException)
        assertTrue(
            "Error should explain the tx isn't fully signed",
            error!!.message!!.contains("isn't fully signed"),
        )
    }

    @Test
    fun fullySignedTransaction_isDecoded() {
        // Block 170: Satoshi -> Hal Finney. Fully signed (P2PK scriptSig present),
        // two outputs (10 BTC + 40 BTC = 50 BTC).
        val signedHex = "0100000001c997a5e56e104102fa209c6a852dd90660a20b2d9c352423edce2585" +
            "7fcd3704000000004847304402204e45e16932b8af514961a1d3a1a25fdf3f4f7732e9d624c6" +
            "c61548ab5fb8cd410220181522ec8eca07de4860a4acdd12909d831cc56cbbac4622082221a8" +
            "768d1d0901ffffffff0200ca9a3b00000000434104ae1a62fe09c5f51b13905f07f06b99a2f7" +
            "159b2225f374cd378d71302fa28414e7aab37397f554a7df5f142c21c1b7303b8a0626f1bade" +
            "d5c72a704f7e6cd84cba00286bee0000000043410411db93e1dcdb8a016b49840f8c53bc1eb6" +
            "8a382e97b1482ecad7b148a6909a5cb2e0eaddfb84ccf9744464f82e160bfa9b8b64f9d4c03f" +
            "999b8643f656b412a3ac00000000"

        val result = decoder.decode(signedHex.hexToBytes(), ScanTransport.SINGLE)

        assertTrue("Signed tx should decode", result.isSuccess)
        val decoded = result.getOrThrow()
        assertEquals(
            "f4184fc596403b9d638783cf57adfe4c75c605f6356fbc91338530e9831e9e16",
            decoded.txid,
        )
        assertEquals(1, decoded.inputCount)
        assertEquals(2, decoded.outputCount)
        assertEquals(5_000_000_000L, decoded.totalOutSats)
        assertEquals(PayloadType.TRANSACTION, decoded.payloadType)
        assertEquals(ScanTransport.SINGLE, decoded.transport)
        // A raw transaction carries no input amounts, so the fee is unknown.
        assertNull(decoded.feeSats)
    }

    @Test
    fun psbtMagicBytes_withGarbageBody_reportsPsbtError() {
        // PSBT magic (0x70736274FF) routes to the PSBT path; the rest is invalid.
        val psbtGarbage = byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte(), 0x00, 0x01, 0x02)

        val result = decoder.decode(psbtGarbage, ScanTransport.SINGLE)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TransactionDecodeException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("PSBT"))
    }

    @Test
    fun nonTransactionBytes_reportsTransactionError() {
        val garbage = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val result = decoder.decode(garbage, ScanTransport.SINGLE)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TransactionDecodeException)
    }

    private fun String.hexToBytes(): ByteArray =
        ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
