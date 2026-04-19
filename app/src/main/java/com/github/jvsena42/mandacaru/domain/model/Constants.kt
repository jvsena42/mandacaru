package com.github.jvsena42.mandacaru.domain.model

object Constants {
    const val ELECTRUM_ADDRESS = "127.0.0.1:50001"
    const val RPC_PORT_MAINNET = "8332"
    const val RPC_PORT_TESTNET = "18332"
    const val RPC_PORT_TESTNET_4 = "48332"
    const val RPC_PORT_SIGNET = "38332"
    const val RPC_PORT_REGTEST = "18443"
    const val ELECTRUM_PORT_MAINNET = "50001"
    const val ELECTRUM_PORT_TESTNET = "30001"
    const val ELECTRUM_PORT_TESTNET_4 = "40001"
    const val ELECTRUM_PORT_SIGNET = "60001"
    const val ELECTRUM_PORT_REGTEST = "20001"

    // Known Utreexo bridge nodes, keyed by FlorestaNetwork enum `.name` (e.g. "BITCOIN").
    // Addresses taken from Floresta/crates/floresta-wire/seeds/mainnet_seeds.json
    // (entries with UTREEXO flag, bit 12) plus upstream commit 7afd8d2 which
    // added Casa21's signet bridge. Only live hosts are listed here.
    private val UTREEXO_BRIDGES: Map<String, List<String>> = mapOf(
        "BITCOIN" to listOf(
            "189.44.63.101:8333",
            "195.26.240.213:8333",
        ),
        "SIGNET" to listOf(
            "189.44.63.101:38333",
        ),
    )

    fun utreexoBridgesFor(network: String): List<String> =
        UTREEXO_BRIDGES[network].orEmpty()
}
