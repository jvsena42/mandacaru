package com.github.jvsena42.mandacaru.domain.floresta

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean

    suspend fun dumpUtreexoState(): Result<String>

    /** Must be called after [stop] has returned. Wipes chaindata + cfilters, preserves the wallet DB. */
    suspend fun prepareForSnapshotImport(): Result<Unit>

    /**
     * Must be called after [stop] has returned. Deletes the watch-only wallet cache
     * (the kv/sled store) while preserving chaindata + cfilters, so the wallet can be
     * rebuilt from scratch by reloading the descriptor(s). Fixes an already-corrupted
     * cache (e.g. balances double-counted before the idempotency fix landed).
     */
    suspend fun clearWalletCache(): Result<Unit>
}
