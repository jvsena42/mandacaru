package com.github.jvsena42.mandacaru.data.geoip

import com.github.jvsena42.mandacaru.domain.geoip.PeerAddressParser
import com.github.jvsena42.mandacaru.domain.geoip.PeerCountryLookup
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Resolves peer addresses against the local [GeoIpDatabase].
 *
 * The node screen repolls every 10 seconds over a stable peer set, so results are memoized
 * per address; only genuinely new peers touch the database.
 */
class MmdbPeerCountryLookup(
    private val database: GeoIpDatabase,
) : PeerCountryLookup {

    private val cacheMutex = Mutex()
    private val cache = object : LinkedHashMap<String, String?>(
        INITIAL_CAPACITY,
        LOAD_FACTOR,
        /* accessOrder = */ true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?): Boolean =
            size > MAX_ENTRIES
    }

    override suspend fun countryCode(address: String): String? {
        cacheMutex.withLock {
            if (cache.containsKey(address)) return cache[address]
        }

        // Sampled *before* the lookup: reading it afterwards would let a lookup that started
        // with no database — and so could not resolve — observe the just-installed file and
        // cache its miss forever, which is the very thing this guard exists to prevent.
        val hadDatabase = database.exists()
        val resolved = PeerAddressParser.parse(address)?.let { database.countryCode(it) }

        // Only remember misses once the database is actually present, otherwise every peer
        // seen before the first download would be pinned to "no flag" for the session.
        if (resolved != null || hadDatabase) {
            cacheMutex.withLock { cache[address] = resolved }
        }
        return resolved
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val MAX_ENTRIES = 256
    }
}
