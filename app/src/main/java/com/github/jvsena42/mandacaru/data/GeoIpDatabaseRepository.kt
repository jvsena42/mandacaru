package com.github.jvsena42.mandacaru.data

/**
 * Keeps the local GeoIP database current. Downloading it is the only network contact the
 * flag feature makes — peer addresses themselves are always resolved locally.
 */
interface GeoIpDatabaseRepository {
    suspend fun refresh(force: Boolean = false)
}
