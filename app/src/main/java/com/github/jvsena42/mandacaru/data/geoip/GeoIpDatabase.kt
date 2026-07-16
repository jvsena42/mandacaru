package com.github.jvsena42.mandacaru.data.geoip

import android.util.Log
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.maxmind.db.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress

/**
 * Owns the on-disk MaxMind database and the reader over it.
 *
 * The file is absent until [GeoIpDatabaseRepository] downloads it — that is the normal
 * first-run state, not an error, so every lookup degrades to null rather than throwing. The
 * reader is memory-mapped, which keeps the ~8 MB off the heap.
 */
class GeoIpDatabase(private val databaseFile: File) {

    private val mutex = Mutex()
    private var reader: Reader? = null
    private var openFailed = false

    /**
     * The lookup runs while holding [mutex] — not just the reader hand-out — so that a
     * concurrent [install] cannot close the reader mid-lookup. Lookups take microseconds and
     * only run for a handful of peers per poll, so the serialization costs nothing.
     */
    suspend fun countryCode(address: InetAddress): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val activeReader = readerLocked() ?: return@withLock null
            runSuspendCatching { activeReader.get(address, Map::class.java) }
                .onFailure { Log.w(TAG, "countryCode: lookup failed", it) }
                .getOrNull()
                ?.isoCode()
        }
    }

    /**
     * Validates [candidate] and, only if it is a usable database, swaps it in atomically. The
     * live database is left untouched on any failure, so a truncated download can never
     * replace a working file.
     */
    suspend fun install(candidate: File): Boolean = withContext(Dispatchers.IO) {
        if (!isUsable(candidate)) {
            candidate.delete()
            return@withContext false
        }
        mutex.withLock {
            closeReader()
            val renamed = candidate.renameTo(databaseFile)
            if (!renamed) {
                Log.w(TAG, "install: could not move database into place")
                candidate.delete()
                return@withLock false
            }
            openFailed = false
            true
        }
    }

    fun exists(): Boolean = databaseFile.exists()

    /**
     * A truncated or non-database payload fails in the [Reader] constructor, because an mmdb
     * keeps its metadata at the end of the file. The probe lookup then proves the search tree
     * is actually traversable; the probe resolving to *some* country is deliberately not
     * required, since which addresses a database covers is not ours to assert.
     */
    private fun isUsable(candidate: File): Boolean = runCatching {
        Reader(candidate).use { candidateReader ->
            val type = candidateReader.metadata.databaseType
            require(type.contains(EXPECTED_TYPE_MARKER, ignoreCase = true)) {
                "unexpected database type '$type'"
            }
            candidateReader.get(InetAddress.getByAddress(PROBE_ADDRESS), Map::class.java)
        }
        true
    }.onFailure { Log.w(TAG, "isUsable: rejecting downloaded database", it) }.getOrDefault(false)

    /** Callers must already hold [mutex]. */
    private fun readerLocked(): Reader? {
        reader?.let { return it }
        if (openFailed || !databaseFile.exists()) return null
        return runCatching { Reader(databaseFile, Reader.FileMode.MEMORY_MAPPED) }
            .onSuccess { reader = it }
            .onFailure {
                // A corrupt file would fail on every poll; complain once and stay quiet.
                Log.w(TAG, "readerLocked: could not open database", it)
                openFailed = true
            }
            .getOrNull()
    }

    private fun closeReader() {
        runCatching { reader?.close() }
        reader = null
    }

    private fun Map<*, *>.isoCode(): String? =
        (this[COUNTRY_KEY] as? Map<*, *>)?.get(ISO_CODE_KEY) as? String

    private companion object {
        const val TAG = "GeoIpDatabase"
        const val COUNTRY_KEY = "country"
        const val ISO_CODE_KEY = "iso_code"
        const val EXPECTED_TYPE_MARKER = "country"

        /** 8.8.8.8 — a stable, always-assigned address used to prove the database resolves. */
        val PROBE_ADDRESS = byteArrayOf(8, 8, 8, 8)
    }
}
