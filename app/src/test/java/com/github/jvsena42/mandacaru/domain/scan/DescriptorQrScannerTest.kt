package com.github.jvsena42.mandacaru.domain.scan

import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.UREncoder
import com.sparrowwallet.hummingbird.registry.CryptoHDKey
import com.sparrowwallet.hummingbird.registry.CryptoOutput
import com.sparrowwallet.hummingbird.registry.ScriptExpression
import com.sparrowwallet.hummingbird.registry.UROutputDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptorQrScannerTest {

    private val scanner = DefaultDescriptorQrScanner()

    @Test
    fun `single-frame raw descriptor completes`() {
        val descriptor = "wpkh([abcd1234/84h/0h/0h]$XPUB/<0;1>/*)"

        val state = scanner.ingest(descriptor)

        assertTrue(state is DescriptorScanState.Complete)
        assertEquals(descriptor, (state as DescriptorScanState.Complete).descriptor)
    }

    @Test
    fun `single-frame bare extended key completes`() {
        val state = scanner.ingest(XPUB)

        assertTrue(state is DescriptorScanState.Complete)
        assertEquals(XPUB, (state as DescriptorScanState.Complete).descriptor)
    }

    @Test
    fun `single-frame JSON export completes with the descriptor`() {
        val state = scanner.ingest("""{"descriptor":"wpkh($XPUB/<0;1>/*)"}""")

        assertTrue(state is DescriptorScanState.Complete)
        assertEquals("wpkh($XPUB/<0;1>/*)", (state as DescriptorScanState.Complete).descriptor)
    }

    @Test
    fun `blank input is idle`() {
        assertTrue(scanner.ingest("   ") is DescriptorScanState.Idle)
    }

    @Test
    fun `garbage frame returns an error`() {
        assertTrue(scanner.ingest("definitely not a descriptor") is DescriptorScanState.Error)
    }

    @Test
    fun `bluewallet coordination file over ur bytes assembles a multisig descriptor`() {
        val ur = UR.fromBytes(BLUEWALLET_FILE.toByteArray())

        val descriptor = drainToComplete(UREncoder.encode(ur))

        assertTrue(descriptor.startsWith("wsh(sortedmulti(2,"))
    }

    @Test
    fun `bluewallet coordination file over bbqr assembles a multisig descriptor`() {
        val hex = BLUEWALLET_FILE.toByteArray().joinToString("") { "%02X".format(it) }
        // B$ + encoding H (hex) + file type U (unicode) + total 01 + index 00 + payload
        val part = "B\$HU0100$hex"

        val state = scanner.ingest(part)

        assertTrue(state is DescriptorScanState.Complete)
        assertTrue((state as DescriptorScanState.Complete).descriptor.startsWith("wsh(sortedmulti(2,"))
    }

    @Test
    fun `ur output-descriptor completes with its source`() {
        val source = "wpkh([abcd1234/84h/0h/0h]$XPUB/<0;1>/*)"
        val ur = UROutputDescriptor(source).toUR()

        val descriptor = drainToComplete(UREncoder.encode(ur))

        assertEquals(source, descriptor)
    }

    @Test
    fun `legacy crypto-output ur returns a graceful error`() {
        val key = ByteArray(33) { 0x02 }
        val chainCode = ByteArray(32) { 0x01 }
        val cryptoOutput = CryptoOutput(
            listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH),
            CryptoHDKey(key, chainCode),
        )

        val state = scanner.ingest(UREncoder.encode(cryptoOutput.toUR()))

        assertTrue(state is DescriptorScanState.Error)
    }

    @Test
    fun `reset clears partial ur state`() {
        val ur = UR.fromBytes(ByteArray(180) { it.toByte() })
        val encoder = UREncoder(ur, FRAGMENT_LEN, MIN_FRAGMENT_LEN, 0L)
        scanner.ingest(encoder.nextPart())

        scanner.reset()

        val state = scanner.ingest("wpkh($XPUB/<0;1>/*)")
        assertTrue(state is DescriptorScanState.Complete)
    }

    private fun drainToComplete(singlePart: String): String {
        var state: DescriptorScanState = scanner.ingest(singlePart)
        assertTrue("expected completion, got $state", state is DescriptorScanState.Complete)
        return (state as DescriptorScanState.Complete).descriptor
    }

    private companion object {
        const val FRAGMENT_LEN = 30
        const val MIN_FRAGMENT_LEN = 10
        const val XPUB = "xpub6BosfCnifzxcFwrSzQiqu2DBVTshkCXacvNsWGYJVVhhawA7d4R5WSWGFNbi8Aw6ZRc1brxMyWMzG3DSSSSoekkudhUd9yLb6qx39T9nMdj"
        const val ZPUB1 = "Zpub72NVPmrzYKbwP7Q4bnm59GjzZCCrqoCAmR4yzKbcdHHsKKMUtn8UqggU6VUMgRTqcAubyQ9bn3Tb9n4LB4RnPiEnCqysjCSZY2MCWUMfNsx"
        const val ZPUB2 = "Zpub72NVPmrzYKbwP7Q4bnm59GjzZCCrqoCAmR4yzKbcdHHsKKMUtn8UqggU6VUMgRTqcAubyQ9bn3Tb9n4LB4RnPiEnCqysjCSZY2MCWPY9XYe"

        val BLUEWALLET_FILE = """
            # BlueWallet Multisig setup file
            Name: Test Vault
            Policy: 2 of 2
            Derivation: m/48'/0'/0'/2'
            Format: P2WSH

            ABCD1234: $ZPUB1
            DEAD5678: $ZPUB2
        """.trimIndent()
    }
}
