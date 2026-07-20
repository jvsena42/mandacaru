package com.github.jvsena42.mandacaru.data.floresta

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockHeaderResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression test for the "View latest block" NullPointerException.
 *
 * Tapping "View latest block" calls the `getblockheader` RPC, which Floresta answers
 * in verbose form by default (verbosity defaults to true, see
 * `floresta-node/src/json_rpc/server.rs`). The verbose body is a serialized
 * `corepc_types::v29::GetBlockHeaderVerbose`, whose JSON keys are `merkleroot`,
 * `previousblockhash` (nullable) and `bits` (a hex *string*, e.g. "1702905c").
 *
 * [com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.BlockHeaderResult]
 * previously declared `@SerializedName("merkle_root")`, `@SerializedName("prev_blockhash")`
 * and `bits: Long`. Because Gson populates fields by reflection without honouring Kotlin
 * nullability, the two mismatched keys left the non-null `merkleRoot`/`prevBlockhash`
 * String fields **null**, and `BlockHeaderCard` then dereferenced them -> NPE. When `bits`
 * contained a-f hex digits Gson instead threw `NumberFormatException`, surfacing as a
 * "Failed to parse response" snackbar.
 *
 * These vectors deserialize with the same default [Gson] the app uses
 * (`FlorestaRpcImpl(gson = Gson(), ...)`).
 */
class GetBlockHeaderResponseParsingTest {

    private val gson = Gson()

    @Test
    fun `verbose getblockheader with hex bits deserializes without null fields`() {
        val response = gson.fromJson(HEX_BITS_RESPONSE, GetBlockHeaderResponse::class.java)
        val header = response.result

        assertNotNull("merkleRoot must map from the `merkleroot` JSON key", header.merkleRoot)
        assertNotNull("prevBlockhash must map from the `previousblockhash` JSON key", header.prevBlockhash)
        assertEquals(
            "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
            header.merkleRoot
        )
        assertEquals(
            "00000000000000000002abc123def456abc123def456abc123def456abc123de",
            header.prevBlockhash
        )
        assertEquals("1702905c", header.bits)
        assertEquals(536870912, header.version)
        assertEquals(1699564800L, header.time)
        assertEquals(2083236893L, header.nonce)
    }

    @Test
    fun `verbose getblockheader with all-digit bits deserializes without null fields`() {
        val response = gson.fromJson(DIGIT_BITS_RESPONSE, GetBlockHeaderResponse::class.java)
        val header = response.result

        assertNotNull(header.merkleRoot)
        assertNotNull(header.prevBlockhash)
        assertEquals("170300000", header.bits)
    }

    @Test
    fun `genesis block header without previousblockhash deserializes to null prevBlockhash`() {
        val response = gson.fromJson(GENESIS_RESPONSE, GetBlockHeaderResponse::class.java)
        val header = response.result

        assertNotNull(header.merkleRoot)
        assertNull("Genesis has no previous block, so prevBlockhash must be null", header.prevBlockhash)
    }

    private companion object {
        private const val HEX_BITS_RESPONSE = """
            {"id":1,"jsonrpc":"2.0","result":{
              "hash":"000000000000000000010a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d",
              "confirmations":6,"height":943609,"version":536870912,"versionHex":"20000000",
              "merkleroot":"4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
              "time":1699564800,"mediantime":1699563000,"nonce":2083236893,
              "bits":"1702905c","target":"00000000000000000002905c00000000000000000000000000000000",
              "difficulty":1.0,"chainwork":"00","nTx":2500,
              "previousblockhash":"00000000000000000002abc123def456abc123def456abc123def456abc123de",
              "nextblockhash":"000000000000000000034d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a"}}
        """

        private const val DIGIT_BITS_RESPONSE = """
            {"id":1,"jsonrpc":"2.0","result":{
              "hash":"000000000000000000010a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d",
              "confirmations":6,"height":943610,"version":536870912,"versionHex":"20000000",
              "merkleroot":"4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
              "time":1699564800,"mediantime":1699563000,"nonce":2083236893,
              "bits":"170300000","target":"00","difficulty":1.0,"chainwork":"00","nTx":2500,
              "previousblockhash":"00000000000000000002abc123def456abc123def456abc123def456abc123de"}}
        """

        private const val GENESIS_RESPONSE = """
            {"id":1,"jsonrpc":"2.0","result":{
              "hash":"000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
              "confirmations":943610,"height":0,"version":1,"versionHex":"00000001",
              "merkleroot":"4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
              "time":1231006505,"mediantime":1231006505,"nonce":2083236893,
              "bits":"1d00ffff","target":"00","difficulty":1.0,"chainwork":"00","nTx":1,
              "nextblockhash":"00000000839a8e6886ab5951d76f411475428afc90947ee320161bbf18eb6048"}}
        """
    }
}
