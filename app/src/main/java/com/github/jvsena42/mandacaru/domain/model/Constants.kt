package com.github.jvsena42.mandacaru.domain.model

object Constants {
    const val FLORESTA_VERSION = "0.9.1"
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

    private val UTREEXO_BRIDGES: Map<String, List<String>> = mapOf(
        "BITCOIN" to listOf(
            "45.77.242.77:8333",
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
