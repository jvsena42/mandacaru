package com.github.jvsena42.mandacaru.presentation.utils

import com.florestad.Network
import com.github.jvsena42.mandacaru.domain.model.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The RPC/Electrum port a network maps to drives which daemon endpoint the app
 * talks to. A wrong mapping silently connects to the wrong network's port, so
 * pin every branch.
 */
class NetworkPortMappingTest {

    @Test
    fun `rpc port maps to the network's constant`() {
        assertEquals(Constants.RPC_PORT_MAINNET, Network.BITCOIN.getRpcPort())
        assertEquals(Constants.RPC_PORT_SIGNET, Network.SIGNET.getRpcPort())
        assertEquals(Constants.RPC_PORT_TESTNET, Network.TESTNET.getRpcPort())
        assertEquals(Constants.RPC_PORT_TESTNET_4, Network.TESTNET4.getRpcPort())
        assertEquals(Constants.RPC_PORT_REGTEST, Network.REGTEST.getRpcPort())
    }

    @Test
    fun `every network maps to a distinct rpc port`() {
        val ports = Network.entries.map { it.getRpcPort() }
        assertEquals(Network.entries.size, ports.toSet().size)
    }

    @Test
    fun `electrum port maps to the network's constant`() {
        assertEquals(Constants.ELECTRUM_PORT_MAINNET, Network.BITCOIN.getElectrumPort())
        assertEquals(Constants.ELECTRUM_PORT_SIGNET, Network.SIGNET.getElectrumPort())
        assertEquals(Constants.ELECTRUM_PORT_TESTNET, Network.TESTNET.getElectrumPort())
        assertEquals(Constants.ELECTRUM_PORT_TESTNET_4, Network.TESTNET4.getElectrumPort())
        assertEquals(Constants.ELECTRUM_PORT_REGTEST, Network.REGTEST.getElectrumPort())
    }

    @Test
    fun `getNetwork parses known names and defaults to signet`() {
        assertEquals(Network.BITCOIN, "BITCOIN".getNetwork())
        assertEquals(Network.REGTEST, "REGTEST".getNetwork())
        // Unknown / malformed values fall back to signet rather than crashing.
        assertEquals(Network.SIGNET, "not-a-network".getNetwork())
        assertEquals(Network.SIGNET, "".getNetwork())
    }
}
