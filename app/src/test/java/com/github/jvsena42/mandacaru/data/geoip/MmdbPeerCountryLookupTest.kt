package com.github.jvsena42.mandacaru.data.geoip

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MmdbPeerCountryLookupTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** Resolved off the test classpath rather than the working directory. */
    private fun fixture(): File = File(
        checkNotNull(javaClass.getResource("/geoip/GeoIP2-Country-Test.mmdb")).toURI()
    )

    private val preferences = FakePreferences()

    private fun lookup(file: File = fixture()) =
        MmdbPeerCountryLookup(GeoIpDatabase(file), preferences)

    @Test
    fun `resolves an IPv4 peer address with port`() = runTest {
        assertEquals("GB", lookup().countryCode("81.2.69.160:8333"))
    }

    @Test
    fun `resolves a bracketed IPv6 peer address with port`() = runTest {
        assertEquals("RO", lookup().countryCode("[2a02:d800::1]:8333"))
    }

    @Test
    fun `private and onion peers resolve to null`() = runTest {
        val lookup = lookup()
        assertNull(lookup.countryCode("192.168.1.5:8333"))
        assertNull(lookup.countryCode("127.0.0.1:8333"))
        assertNull(lookup.countryCode("expyuzz4wqqyqhjn.onion:8333"))
    }

    @Test
    fun `repeated lookups return the memoized value`() = runTest {
        val lookup = lookup()
        val first = lookup.countryCode("81.2.69.160:8333")
        val second = lookup.countryCode("81.2.69.160:8333")
        assertEquals("GB", first)
        assertEquals(first, second)
    }

    @Test
    fun `absent database resolves to null without throwing`() = runTest {
        val missing = File(temporaryFolder.root, "absent.mmdb")
        assertNull(lookup(missing).countryCode("81.2.69.160:8333"))
    }

    @Test
    fun `flags are on by default`() = runTest {
        // Nothing stored: the feature must resolve without any preference being written.
        assertEquals("GB", lookup().countryCode("81.2.69.160:8333"))
    }

    @Test
    fun `disabling the setting suppresses lookups`() = runTest {
        val lookup = lookup()
        preferences.setBoolean(PreferenceKeys.GEOIP_FLAGS_ENABLED, false)
        assertNull(lookup.countryCode("81.2.69.160:8333"))
    }

    @Test
    fun `re-enabling restores flags without a restart`() = runTest {
        val lookup = lookup()
        assertEquals("GB", lookup.countryCode("81.2.69.160:8333"))

        preferences.setBoolean(PreferenceKeys.GEOIP_FLAGS_ENABLED, false)
        assertNull(lookup.countryCode("81.2.69.160:8333"))

        // The disabled reads must not have poisoned the cache with nulls.
        preferences.setBoolean(PreferenceKeys.GEOIP_FLAGS_ENABLED, true)
        assertEquals("GB", lookup.countryCode("81.2.69.160:8333"))
    }

    @Test
    fun `misses are not cached while the database is absent, so flags appear after download`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        val database = GeoIpDatabase(target)
        val lookup = MmdbPeerCountryLookup(database, preferences)

        // Before the download there is no database: the peer has no flag.
        assertNull(lookup.countryCode("81.2.69.160:8333"))

        val candidate = temporaryFolder.newFile("candidate.mmdb")
        fixture().copyTo(candidate, overwrite = true)
        database.install(candidate)

        // The next poll must pick the country up rather than serve a cached miss.
        assertEquals("GB", lookup.countryCode("81.2.69.160:8333"))
    }

    private class FakePreferences : PreferencesDataSource {
        private val strings = mutableMapOf<PreferenceKeys, String>()
        private val booleans = mutableMapOf<PreferenceKeys, Boolean>()
        override suspend fun setString(key: PreferenceKeys, value: String) { strings[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String): String =
            strings[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean): Boolean =
            booleans[key] ?: defaultValue
    }
}
