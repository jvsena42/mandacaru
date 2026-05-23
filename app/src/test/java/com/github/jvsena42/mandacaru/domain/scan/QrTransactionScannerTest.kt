package com.github.jvsena42.mandacaru.domain.scan

import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.UREncoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class QrTransactionScannerTest {

    private val scanner = DefaultQrTransactionScanner()

    @Test
    fun `single hex frame completes with decoded bytes`() {
        val bytes = byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x01)

        val state = scanner.ingest("0200000001")

        assertTrue(state is ScanState.Complete)
        state as ScanState.Complete
        assertArrayEquals(bytes, state.payload)
        assertEquals(ScanTransport.SINGLE, state.transport)
    }

    @Test
    fun `single base64 psbt frame completes with decoded bytes`() {
        val bytes = byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte(), 0x01, 0x00)
        val base64 = Base64.getEncoder().encodeToString(bytes)

        val state = scanner.ingest(base64)

        assertTrue(state is ScanState.Complete)
        state as ScanState.Complete
        assertArrayEquals(bytes, state.payload)
        assertEquals(ScanTransport.SINGLE, state.transport)
    }

    @Test
    fun `unrecognized content returns an error`() {
        val state = scanner.ingest("not a valid payload!")

        assertTrue(state is ScanState.Error)
    }

    @Test
    fun `blank input is idle`() {
        assertTrue(scanner.ingest("   ") is ScanState.Idle)
    }

    @Test
    fun `single bbqr part completes with bbqr transport`() {
        val bytes = byteArrayOf(0x70, 0x73, 0x62, 0x74, 0xFF.toByte())

        val state = scanner.ingest("B\$HP0100" + "70736274FF")

        assertTrue(state is ScanState.Complete)
        state as ScanState.Complete
        assertEquals(ScanTransport.BBQR, state.transport)
        assertArrayEquals(bytes, state.payload)
    }

    @Test
    fun `multi-part ur reassembles into the original bytes`() {
        val original = ByteArray(180) { (it * 7).toByte() }
        val encoder = UREncoder(UR.fromBytes(original), FRAGMENT_LEN, MIN_FRAGMENT_LEN, 0L)

        var sawProgress = false
        var state: ScanState = ScanState.Idle
        var guard = 0
        while (state !is ScanState.Complete && guard < MAX_PARTS) {
            state = scanner.ingest(encoder.nextPart())
            if (state is ScanState.InProgress) sawProgress = true
            guard++
        }

        assertTrue("expected progress before completion", sawProgress)
        assertTrue(state is ScanState.Complete)
        state as ScanState.Complete
        assertArrayEquals(original, state.payload)
        assertEquals(ScanTransport.UR, state.transport)
    }

    @Test
    fun `reset clears partial ur state`() {
        val original = ByteArray(180) { it.toByte() }
        val encoder = UREncoder(UR.fromBytes(original), FRAGMENT_LEN, MIN_FRAGMENT_LEN, 0L)

        scanner.ingest(encoder.nextPart())
        scanner.reset()

        // After reset, a single-frame hex is treated independently rather than as a UR fragment.
        val state = scanner.ingest("0200000001")
        assertTrue(state is ScanState.Complete)
        assertEquals(ScanTransport.SINGLE, (state as ScanState.Complete).transport)
    }

    private companion object {
        const val FRAGMENT_LEN = 30
        const val MIN_FRAGMENT_LEN = 10
        const val MAX_PARTS = 200
    }
}
