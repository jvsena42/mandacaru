package com.github.jvsena42.mandacaru.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptorUtilsTest {

    // Real zpub key (BIP84 mainnet) — used in the original bug report
    private val realZpub = "zpub6rFR7y4Q2AijBEqTUquhVz398htDFrtymD9xYYfG1m4wAcvPhXNfE3EfH1r1ADqtfSdVCToUG868RvUUkgDKf31mGDtKsAYz2oz2AGutZYs"

    // --- convertSlip132ToStandard ---

    @Test
    fun `zpub is converted to xpub`() {
        val result = DescriptorUtils.convertSlip132ToStandard(realZpub)
        assertTrue("Should start with xpub, got: $result", result.startsWith("xpub"))
    }

    @Test
    fun `zpub conversion produces valid xpub that roundtrips`() {
        val xpub = DescriptorUtils.convertSlip132ToStandard(realZpub)
        // Converting an xpub should return it unchanged (it's already standard)
        val roundtrip = DescriptorUtils.convertSlip132ToStandard(xpub)
        assertEquals(xpub, roundtrip)
    }

    @Test
    fun `xpub is returned unchanged`() {
        val xpub = "xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG"
        val result = DescriptorUtils.convertSlip132ToStandard(xpub)
        assertEquals(xpub, result)
    }

    @Test
    fun `tpub is returned unchanged`() {
        val tpub = "tpubDDgpGUUjzTqLqQL9WzPvMDTKyD95AUcwWohMWWoj5kqGU7VLSZ3ju9ZtHRN4ofK6KNaZsTSpB6yGrFuV1V4yVgcwksueuFW3YnKxwoNqb3V"
        val result = DescriptorUtils.convertSlip132ToStandard(tpub)
        assertEquals(tpub, result)
    }

    @Test
    fun `invalid base58 string is returned unchanged`() {
        val invalid = "not-a-valid-key-0OIl"
        val result = DescriptorUtils.convertSlip132ToStandard(invalid)
        assertEquals(invalid, result)
    }

    @Test
    fun `empty string conversion returns unchanged`() {
        val result = DescriptorUtils.convertSlip132ToStandard("")
        assertEquals("", result)
    }

    @Test
    fun `key with bad checksum is returned unchanged`() {
        // Corrupt last character of a valid zpub
        val corrupted = realZpub.dropLast(1) + "X"
        val result = DescriptorUtils.convertSlip132ToStandard(corrupted)
        assertEquals(corrupted, result)
    }

    // --- wrapDescriptorIfNeeded ---

    @Test
    fun `zpub without path is converted to xpub and wrapped as wpkh`() {
        val result = DescriptorUtils.wrapDescriptorIfNeeded(realZpub)
        assertTrue("Should be wpkh wrapped with xpub, got: $result", result.startsWith("wpkh(xpub"))
        assertTrue("Should have default derivation path", result.endsWith("/<0;1>/*)"))
    }

    @Test
    fun `xpub without path is wrapped as pkh with default derivation`() {
        val xpub = "xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(xpub)
        assertEquals("pkh($xpub/<0;1>/*)", result)
    }

    @Test
    fun `tpub without path is wrapped as pkh with default derivation`() {
        val tpub = "tpubDDgpGUUjzTqLqQL9WzPvMDTKyD95AUcwWohMWWoj5kqGU7VLSZ3ju9ZtHRN4ofK6KNaZsTSpB6yGrFuV1V4yVgcwksueuFW3YnKxwoNqb3V"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(tpub)
        assertEquals("pkh($tpub/<0;1>/*)", result)
    }

    @Test
    fun `xpub with existing derivation path does not get default path added`() {
        val xpubWithPath = "xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG/0/*"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(xpubWithPath)
        assertEquals("pkh($xpubWithPath)", result)
    }

    @Test
    fun `xpub with multipath derivation does not get default path added`() {
        val xpubWithPath = "xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG/<0;1>/*"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(xpubWithPath)
        assertEquals("pkh($xpubWithPath)", result)
    }

    @Test
    fun `full wpkh descriptor is passed through unchanged`() {
        val descriptor = "wpkh([a5b13c0e/84h/0h/0h]xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG/<0;1>/*)#n8sgapuv"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(descriptor)
        assertEquals(descriptor, result)
    }

    @Test
    fun `sh wpkh descriptor is passed through unchanged`() {
        val descriptor = "sh(wpkh(xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG/0/*))"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(descriptor)
        assertEquals(descriptor, result)
    }

    @Test
    fun `wsh sortedmulti descriptor is passed through unchanged`() {
        val descriptor = "wsh(sortedmulti(1,[6f826a6a/48h/0h/0h/2h]xpub6DsY48BAsvEMTRPbeSTu9jZXqEsTKr5T86WbRbXHp2gEVCNR3hALnMorFawVwnnHMMfjbyY8We9B4beh1fxqhcv6kgSeLgQxeXDqv3DaW7m/<0;1>/*))"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(descriptor)
        assertEquals(descriptor, result)
    }

    @Test
    fun `unrecognized key prefix is passed through unchanged`() {
        val unknown = "abcdef123456"
        val result = DescriptorUtils.wrapDescriptorIfNeeded(unknown)
        assertEquals(unknown, result)
    }

    @Test
    fun `empty string is passed through unchanged`() {
        val result = DescriptorUtils.wrapDescriptorIfNeeded("")
        assertEquals("", result)
    }

    // --- isPrivateKey ---

    @Test
    fun `xprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("xprv9s21ZrQH143K3GJpoapnV8SFfuZcECeGTqTKP9HYSnmgYfq6"))
    }

    @Test
    fun `yprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("yprv9s21ZrQH143K3GJpoapnV8SFfuZcECeGTqTKP9HYSnmgYfq6"))
    }

    @Test
    fun `zprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("zprv9s21ZrQH143K3GJpoapnV8SFfuZcECeGTqTKP9HYSnmgYfq6"))
    }

    @Test
    fun `tprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("tprv8ZgxMBicQKsPd7Uf69XL1XwhmjXhN7PBzYA1zG3"))
    }

    @Test
    fun `uprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("uprv8ZgxMBicQKsPd7Uf69XL1XwhmjXhN7PBzYA1zG3"))
    }

    @Test
    fun `vprv is detected as private key`() {
        assertTrue(DescriptorUtils.isPrivateKey("vprv8ZgxMBicQKsPd7Uf69XL1XwhmjXhN7PBzYA1zG3"))
    }

    @Test
    fun `xpub is not detected as private key`() {
        assertFalse(DescriptorUtils.isPrivateKey("xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gfAmWewYFzjNYFgRiCjoB9WWEuJQiyYGCdZvUTwPEUPL9pPabT8bkbiD9Po47XG"))
    }

    @Test
    fun `zpub is not detected as private key`() {
        assertFalse(DescriptorUtils.isPrivateKey(realZpub))
    }

    @Test
    fun `empty string is not detected as private key`() {
        assertFalse(DescriptorUtils.isPrivateKey(""))
    }

    @Test
    fun `full descriptor is not a false positive for private key`() {
        assertFalse(DescriptorUtils.isPrivateKey("wpkh(xpub6CFy3kRXorC3NMTt8qrsY9ucUfxVLXyFQ49JSLm3iEG5gf)"))
    }

    // --- extractDescriptor ---

    @Test
    fun `extractDescriptor returns a raw descriptor unchanged`() {
        val desc = "wpkh([abcd1234/84h/0h/0h]$XPUB1/<0;1>/*)"
        assertEquals(desc, DescriptorUtils.extractDescriptor(desc))
    }

    @Test
    fun `extractDescriptor returns a bare extended key`() {
        assertEquals(XPUB1, DescriptorUtils.extractDescriptor(XPUB1))
        assertEquals(realZpub, DescriptorUtils.extractDescriptor(realZpub))
    }

    @Test
    fun `extractDescriptor pulls the descriptor out of a JSON export`() {
        val json = """{"label":"Cold","descriptor":"wpkh($XPUB1/<0;1>/*)"}"""
        assertEquals("wpkh($XPUB1/<0;1>/*)", DescriptorUtils.extractDescriptor(json))
    }

    @Test
    fun `extractDescriptor finds a nested descriptor in JSON`() {
        val json = """{"bip84":{"name":"p2wpkh","desc":"wpkh($XPUB1/0/*)"}}"""
        assertEquals("wpkh($XPUB1/0/*)", DescriptorUtils.extractDescriptor(json))
    }

    @Test
    fun `extractDescriptor rejects garbage`() {
        assertEquals(null, DescriptorUtils.extractDescriptor("not a descriptor at all"))
        assertEquals(null, DescriptorUtils.extractDescriptor(""))
    }

    // --- electrumKeyFor (SLIP-132 export; ground truth = official BIP test vectors) ---

    @Test
    fun `electrumKeyFor produces the BIP84 zpub for a native segwit descriptor`() {
        val xpub = DescriptorUtils.convertSlip132ToStandard(realZpub)
        val descriptor = "wpkh([00000000/84h/0h/0h]$xpub/<0;1>/*)"
        assertEquals(realZpub, DescriptorUtils.electrumKeyFor(descriptor))
    }

    @Test
    fun `electrumKeyFor produces the BIP49 upub for a testnet nested segwit descriptor`() {
        val tpub = DescriptorUtils.convertSlip132ToStandard(BIP49_TESTNET_UPUB)
        val descriptor = "sh(wpkh($tpub/<0;1>/*))"
        assertEquals(BIP49_TESTNET_UPUB, DescriptorUtils.electrumKeyFor(descriptor))
    }

    @Test
    fun `electrumKeyFor returns the xpub unchanged for a legacy descriptor`() {
        assertEquals(BIP32_XPUB, DescriptorUtils.electrumKeyFor("pkh($BIP32_XPUB/<0;1>/*)"))
    }

    @Test
    fun `electrumKeyFor roundtrips back to the descriptor's standard key`() {
        val xpub = DescriptorUtils.convertSlip132ToStandard(realZpub)
        val zpub = DescriptorUtils.electrumKeyFor("wpkh($xpub/<0;1>/*)")!!
        assertEquals(xpub, DescriptorUtils.convertSlip132ToStandard(zpub))
    }

    @Test
    fun `electrumKeyFor returns null for multisig`() {
        assertEquals(null, DescriptorUtils.electrumKeyFor(DescriptorUtils.parseMultisigSetupFile(BLUEWALLET_FILE)!!))
    }

    @Test
    fun `electrumKeyFor returns null for taproot`() {
        assertEquals(null, DescriptorUtils.electrumKeyFor("tr($XPUB1/<0;1>/*)"))
    }

    @Test
    fun `electrumKeyFor returns null when no extended key is present`() {
        assertEquals(null, DescriptorUtils.electrumKeyFor("addr(bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4)"))
    }

    // --- parseMultisigSetupFile (BlueWallet / Coldcard) ---

    @Test
    fun `parseMultisigSetupFile assembles a sortedmulti P2WSH descriptor`() {
        val expected = "wsh(sortedmulti(2," +
            "[abcd1234/48h/0h/0h/2h]$XPUB1/<0;1>/*," +
            "[dead5678/48h/0h/0h/2h]$XPUB2/<0;1>/*))"
        assertEquals(expected, DescriptorUtils.parseMultisigSetupFile(BLUEWALLET_FILE))
    }

    @Test
    fun `extractDescriptor routes a BlueWallet setup file through the parser`() {
        assertTrue(
            DescriptorUtils.extractDescriptor(BLUEWALLET_FILE)!!.startsWith("wsh(sortedmulti(2,"),
        )
    }

    @Test
    fun `parseMultisigSetupFile honours P2SH-P2WSH format`() {
        val file = BLUEWALLET_FILE.replace("Format: P2WSH", "Format: P2SH-P2WSH")
        assertTrue(DescriptorUtils.parseMultisigSetupFile(file)!!.startsWith("sh(wsh(sortedmulti(2,"))
    }

    @Test
    fun `parseMultisigSetupFile honours P2SH format`() {
        val file = BLUEWALLET_FILE.replace("Format: P2WSH", "Format: P2SH")
        assertTrue(DescriptorUtils.parseMultisigSetupFile(file)!!.startsWith("sh(sortedmulti(2,"))
    }

    @Test
    fun `parseMultisigSetupFile converts multisig Zpub keys to xpub`() {
        val result = DescriptorUtils.parseMultisigSetupFile(BLUEWALLET_FILE)!!
        assertTrue("expected converted xpub keys, got: $result", result.contains(XPUB1))
        assertFalse(result.contains("Zpub"))
    }

    @Test
    fun `parseMultisigSetupFile returns null without a policy`() {
        val file = BLUEWALLET_FILE.lineSequence().filterNot { it.startsWith("Policy") }
            .joinToString("\n")
        assertEquals(null, DescriptorUtils.parseMultisigSetupFile(file))
    }

    // --- summarize ---

    @Test
    fun `summarize reports native segwit and key origin`() {
        val summary = DescriptorUtils.summarize("wpkh([abcd1234/84h/0h/0h]$XPUB1/<0;1>/*)")
        assertEquals("Native SegWit (P2WPKH)", summary.scriptType)
        assertEquals("abcd1234", summary.fingerprint)
        assertEquals("84h/0h/0h", summary.derivationPath)
        assertEquals(null, summary.multisig)
    }

    @Test
    fun `summarize reports nested segwit`() {
        val summary = DescriptorUtils.summarize("sh(wpkh($XPUB1/<0;1>/*))")
        assertEquals("Nested SegWit (P2SH-P2WPKH)", summary.scriptType)
    }

    @Test
    fun `summarize reports taproot`() {
        assertEquals("Taproot (P2TR)", DescriptorUtils.summarize("tr($XPUB1/<0;1>/*)").scriptType)
    }

    @Test
    fun `summarize reports multisig policy and cosigner count`() {
        val descriptor = DescriptorUtils.parseMultisigSetupFile(BLUEWALLET_FILE)!!
        val summary = DescriptorUtils.summarize(descriptor)
        assertEquals("Multisig Native SegWit (P2WSH)", summary.scriptType)
        assertEquals("2-of-2", summary.multisig)
    }

    private companion object {
        // BIP-49 "Test vectors" §, Account 0 testnet extended public key.
        const val BIP49_TESTNET_UPUB = "upub5EFU65HtV5TeiSHmZZm7FUffBGy8UKeqp7vw43jYbvZPpoVsgU93oac7Wk3u6moKegAEWtGNF8DehrnHtv21XXEMYRUocHqguyjknFHYfgY"

        // BIP-32 "Test vector 1", chain m extended public key.
        const val BIP32_XPUB = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"

        const val XPUB1 = "xpub6BosfCnifzxcFwrSzQiqu2DBVTshkCXacvNsWGYJVVhhawA7d4R5WSWGFNbi8Aw6ZRc1brxMyWMzG3DSSSSoekkudhUd9yLb6qx39T9nMdj"
        const val XPUB2 = "xpub6BosfCnifzxcFwrSzQiqu2DBVTshkCXacvNsWGYJVVhhawA7d4R5WSWGFNbi8Aw6ZRc1brxMyWMzG3DSSSSoekkudhUd9yLb6qx39HjUYTz"
        const val ZPUB1 = "Zpub72NVPmrzYKbwP7Q4bnm59GjzZCCrqoCAmR4yzKbcdHHsKKMUtn8UqggU6VUMgRTqcAubyQ9bn3Tb9n4LB4RnPiEnCqysjCSZY2MCWUMfNsx"
        const val ZPUB2 = "Zpub72NVPmrzYKbwP7Q4bnm59GjzZCCrqoCAmR4yzKbcdHHsKKMUtn8UqggU6VUMgRTqcAubyQ9bn3Tb9n4LB4RnPiEnCqysjCSZY2MCWPY9XYe"

        val BLUEWALLET_FILE = """
            # BlueWallet Multisig setup file
            # this file contains only public keys and is safe to
            # distribute among cosigners
            #
            Name: Test Vault
            Policy: 2 of 2
            Derivation: m/48'/0'/0'/2'
            Format: P2WSH

            ABCD1234: $ZPUB1
            DEAD5678: $ZPUB2
        """.trimIndent()
    }
}
