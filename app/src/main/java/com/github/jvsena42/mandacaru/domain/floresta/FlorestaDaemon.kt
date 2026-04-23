package com.github.jvsena42.mandacaru.domain.floresta

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean

    suspend fun dumpUtreexoState(): Result<String>

    /** Must be called after [stop] has returned. Wipes chaindata + cfilters, preserves the wallet DB. */
    suspend fun prepareForSnapshotImport(): Result<Unit>
}
