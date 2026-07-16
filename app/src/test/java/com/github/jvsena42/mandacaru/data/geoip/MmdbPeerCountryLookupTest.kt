package com.github.jvsena42.mandacaru.data.geoip

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

    private fun fixture(): File = File("src/test/resources/geoip/GeoIP2-Country-Test.mmdb")

    private fun lookup(file: File = fixture()) = MmdbPeerCountryLookup(GeoIpDatabase(file))

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
    fun `misses are not cached while the database is absent, so flags appear after download`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        val database = GeoIpDatabase(target)
        val lookup = MmdbPeerCountryLookup(database)

        // Before the download there is no database: the peer has no flag.
        assertNull(lookup.countryCode("81.2.69.160:8333"))

        val candidate = temporaryFolder.newFile("candidate.mmdb")
        fixture().copyTo(candidate, overwrite = true)
        database.install(candidate)

        // The next poll must pick the country up rather than serve a cached miss.
        assertEquals("GB", lookup.countryCode("81.2.69.160:8333"))
    }
}
