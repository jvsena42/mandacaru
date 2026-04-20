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
    // Each entry must be a peer confirmed to advertise NODE_UTREEXO (service flag bit 12).
    //
    // BITCOIN is intentionally empty: a full v2 sweep of the 7 UTREEXO-flagged entries in
    // Floresta/crates/floresta-wire/seeds/mainnet_seeds.json found zero live utreexo peers
    // on mainnet (5 are unreachable; 195.26.240.213 now runs vanilla Bitcoin Core 30.0.0;
    // Casa21's 189.44.63.101:8333 accepts TCP but never completes the v2 handshake).
    //
    // 1.228.21.110 (testnet:18333 / signet:38333) runs `utreexod 0.5.0` and is the only
    // verified live Utreexo peer we found across all 7 seed-file entries.
    private val UTREEXO_BRIDGES: Map<String, List<String>> = mapOf(
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
