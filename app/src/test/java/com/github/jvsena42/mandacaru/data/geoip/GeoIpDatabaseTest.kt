package com.github.jvsena42.mandacaru.data.geoip

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.InetAddress

/**
 * Exercises the real maxmind-db reader against the upstream GeoIP2-Country test fixture
 * (Apache-2.0), so the lookup path is covered end-to-end without a device or AssetManager.
 */
class GeoIpDatabaseTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** Resolved off the test classpath rather than the working directory. */
    private fun fixture(): File = File(
        checkNotNull(javaClass.getResource(FIXTURE)) { "test fixture missing: $FIXTURE" }.toURI()
    )

    private fun databaseAt(file: File) = GeoIpDatabase(file)

    private fun ip(value: String): InetAddress =
        InetAddress.getByAddress(InetAddress.getByName(value).address)

    @Test
    fun `fixture is present`() {
        assertTrue("test fixture missing: ${fixture().absolutePath}", fixture().exists())
    }

    private companion object {
        const val FIXTURE = "/geoip/GeoIP2-Country-Test.mmdb"
    }

    @Test
    fun `resolves IPv4 to its country`() = runTest {
        val database = databaseAt(fixture())
        assertEquals("GB", database.countryCode(ip("81.2.69.160")))
        assertEquals("SE", database.countryCode(ip("89.160.20.128")))
    }

    @Test
    fun `resolves IPv6 to its country`() = runTest {
        assertEquals("RO", databaseAt(fixture()).countryCode(ip("2a02:d800::1")))
    }

    @Test
    fun `unknown and loopback addresses resolve to null`() = runTest {
        val database = databaseAt(fixture())
        assertNull(database.countryCode(ip("127.0.0.1")))
        assertNull(database.countryCode(ip("192.168.1.5")))
    }

    @Test
    fun `absent database resolves to null instead of throwing`() = runTest {
        val missing = File(temporaryFolder.root, "nope.mmdb")
        val database = databaseAt(missing)
        assertFalse(database.exists())
        assertNull(database.countryCode(ip("81.2.69.160")))
    }

    @Test
    fun `corrupt database resolves to null instead of throwing`() = runTest {
        val corrupt = temporaryFolder.newFile("corrupt.mmdb")
        corrupt.writeBytes(ByteArray(2048) { 0x41 })
        assertNull(databaseAt(corrupt).countryCode(ip("81.2.69.160")))
    }

    @Test
    fun `install accepts a valid database and makes it readable`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        val database = databaseAt(target)
        val candidate = temporaryFolder.newFile("candidate.mmdb")
        fixture().copyTo(candidate, overwrite = true)

        assertTrue(database.install(candidate))
        assertTrue(target.exists())
        assertEquals("GB", database.countryCode(ip("81.2.69.160")))
    }

    @Test
    fun `install rejects a truncated download and leaves the live database untouched`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        val database = databaseAt(target)
        fixture().copyTo(target, overwrite = true)

        val truncated = temporaryFolder.newFile("truncated.mmdb")
        truncated.writeBytes(fixture().readBytes().copyOfRange(0, 4096))

        assertFalse(database.install(truncated))
        assertFalse("candidate should be cleaned up", truncated.exists())
        // The working database still resolves.
        assertEquals("GB", database.countryCode(ip("81.2.69.160")))
    }

    @Test
    fun `install rejects a non-database payload`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        val database = databaseAt(target)
        val html = temporaryFolder.newFile("error.html")
        html.writeText("<html><body>404 Not Found</body></html>")

        assertFalse(database.install(html))
        assertFalse(target.exists())
    }
}
