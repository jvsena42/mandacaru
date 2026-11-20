package com.github.jvsena42.mandacaru.domain.floresta

import java.io.File

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()

    /**
     * Stops the node before the process ends. Waits for a start in progress to finish rather
     * than killing it mid-write, and no start runs after it: the process is going away.
     */
    suspend fun stopForExit() = stop()
    fun isRunning(): Boolean

    /** The SOCKS5 proxy the running node was started with, or null when it connects directly. */
    fun runningProxy(): String? = null

    /** The JSON-RPC cookie the running node wrote, or null while it isn't running. */
    fun rpcCookieFile(): File?

    suspend fun dumpUtreexoState(): Result<String>

    /** Must be called after [stop] has returned. Wipes chaindata + cfilters, preserves the wallet DB. */
    suspend fun prepareForSnapshotImport(): Result<Unit>
}
