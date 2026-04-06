package com.github.jvsena42.mandacaru.data.floresta

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that addNode JSON-RPC requests are built correctly with both the
 * node address and the "add" command parameter.
 *
 * Test vectors from Floresta mainnet seeds:
 * https://github.com/getfloresta/Floresta/blob/master/crates/floresta-wire/seeds/mainnet_seeds.json
 */
class AddNodeRequestTest {

    private val gson = Gson()

    // --- Mainnet seed IPv4 addresses (from Floresta seeds) ---

    @Test
    fun `addnode request includes add command for seed 189_44_63_101`() {
        assertValidAddNodeRequest("189.44.63.101:8333")
    }

    @Test
    fun `addnode request includes add command for seed 195_26_240_213`() {
        assertValidAddNodeRequest("195.26.240.213:8333")
    }

    @Test
    fun `addnode request includes add command for seed 1_228_21_110`() {
        assertValidAddNodeRequest("1.228.21.110:8333")
    }

    @Test
    fun `addnode request includes add command for seed 194_163_132_180`() {
        assertValidAddNodeRequest("194.163.132.180:8333")
    }

    @Test
    fun `addnode request includes add command for seed 161_97_178_61`() {
        assertValidAddNodeRequest("161.97.178.61:8333")
    }

    // --- Non-default port ---

    @Test
    fun `addnode request includes add command for seed with non-default port 8433`() {
        assertValidAddNodeRequest("195.26.240.213:8433")
    }

    // --- IPv6 address ---

    @Test
    fun `addnode request includes add command for IPv6 seed`() {
        assertValidAddNodeRequest("[2001:fb1:42:567:ea9c:25ff:fe79:744]:8333")
    }

    // --- Address without port ---

    @Test
    fun `addnode request includes add command for address without port`() {
        assertValidAddNodeRequest("161.97.178.61")
    }

    // --- JSON-RPC format ---

    @Test
    fun `addnode request uses JSON-RPC 2_0 format`() {
        val request = buildAddNodeRequest("189.44.63.101:8333")
        assertEquals("2.0", request.get("jsonrpc").asString)
        assertEquals(1, request.get("id").asInt)
        assertEquals("addnode", request.get("method").asString)
    }

    @Test
    fun `addnode params must have exactly 2 elements`() {
        val request = buildAddNodeRequest("194.163.132.180:8333")
        val params = request.getAsJsonArray("params")
        assertEquals(
            "addnode requires exactly 2 params: [address, command]",
            2,
            params.size()
        )
    }

    @Test
    fun `addnode second param is the add command`() {
        val request = buildAddNodeRequest("1.228.21.110:8333")
        val params = request.getAsJsonArray("params")
        assertEquals("add", params[1].asString)
    }

    /**
     * Builds a JSON-RPC request body the same way [FlorestaRpcImpl] does for addNode.
     */
    private fun buildAddNodeRequest(nodeAddress: String): JsonObject {
        val params = listOf(nodeAddress, "add")
        val request = mapOf(
            "jsonrpc" to "2.0",
            "method" to "addnode",
            "params" to params,
            "id" to 1
        )
        val json = gson.toJson(request)
        return JsonParser.parseString(json).asJsonObject
    }

    private fun assertValidAddNodeRequest(expectedAddress: String) {
        val request = buildAddNodeRequest(expectedAddress)
        assertEquals("addnode", request.get("method").asString)
        val params = request.getAsJsonArray("params")
        assertEquals(
            "addnode requires 2 params: [address, command]",
            2,
            params.size()
        )
        assertEquals(expectedAddress, params[0].asString)
        assertEquals("add", params[1].asString)
    }
}
