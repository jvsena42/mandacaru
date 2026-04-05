package com.github.jvsena42.mandacaru.domain.floresta

interface FlorestaDaemon {
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
}
