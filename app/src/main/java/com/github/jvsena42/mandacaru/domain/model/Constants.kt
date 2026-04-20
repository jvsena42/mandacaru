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

    // Known Utreexo bridge nodes, keyed by FlorestaNetwork enum `.name`.
    // Each entry must be a peer confirmed to advertise NODE_UTREEXO (bit 12).
    //
    // 195.26.240.213:8433 — luisschwab's Utreexo bridge (confirmed by operator
    //   in getfloresta/Floresta#971 comment 4284241912). The :8333 entry that
    //   appears in Floresta's mainnet_seeds.json is his regular Bitcoin Core
    //   and should not be treated as a Utreexo peer.
    // 1.228.21.110 — runs `utreexod 0.5.0`; advertises UTREEXO + UTREEXO_ARCHIVE
    //   on testnet (18333) and signet (38333). Mainnet port (8333) is closed.
    // 189.44.63.101:38333 (Casa21) — signet bridge added upstream in 7afd8d2.
    private val UTREEXO_BRIDGES: Map<String, List<String>> = mapOf(
        "BITCOIN" to listOf(
            "195.26.240.213:8433",
        ),
        "SIGNET" to listOf(
            "1.228.21.110:38333",
            "189.44.63.101:38333",
        ),
        "TESTNET" to listOf(
            "1.228.21.110:18333",
        ),
    )

    fun utreexoBridgesFor(network: String): List<String> =
        UTREEXO_BRIDGES[network].orEmpty()
}
