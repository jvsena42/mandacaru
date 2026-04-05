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
}
