package com.github.jvsena42.mandacaru.domain.geoip

/**
 * Resolves a peer address to a country, entirely offline against a local database. Peer
 * addresses are never sent anywhere.
 */
fun interface PeerCountryLookup {
    /**
     * ISO 3166-1 alpha-2 code, or null when the address is private, unparseable, absent from
     * the database, or the database has not been downloaded yet. Never throws.
     */
    suspend fun countryCode(address: String): String?
}
