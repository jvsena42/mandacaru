package com.github.jvsena42.floresta_node.domain.floresta

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
}

/**
 * Platform-specific factory function to create FlorestaDaemon
 */
expect fun createFlorestaDaemon(
    datadir: String,
    network: String
): FlorestaDaemon
