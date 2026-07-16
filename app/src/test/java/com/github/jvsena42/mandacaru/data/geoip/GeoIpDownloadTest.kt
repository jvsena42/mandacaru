package com.github.jvsena42.mandacaru.data.geoip

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.geoip.GeoIpUrl
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.time.YearMonth
import java.util.zip.GZIPOutputStream

/**
 * Drives the real download path against a local HTTP server (JDK built-in, no extra
 * dependency), covering what the offline tests cannot: that a refresh **replaces** the
 * database and leaves no scratch files behind.
 */
class GeoIpDownloadTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: HttpServer
    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private val preferences = FakePreferences()

    /** month stamp -> gzipped body, or absent to serve a 404. */
    private val published = mutableMapOf<String, ByteArray>()

    private fun fixtureBytes(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/geoip/$name")).use { it.readBytes() }

    private fun gzip(raw: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw) }
        return out.toByteArray()
    }

    @Before
    fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        cacheDir = temporaryFolder.newFolder("cache")
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val stamp = exchange.requestURI.path.removePrefix("/")
            val body = published[stamp]
            if (body == null) {
                exchange.sendResponseHeaders(404, -1)
            } else {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            exchange.close()
        }
        server.start()
    }

    @After
    fun tearDown() = server.stop(0)

    private fun databaseFile() = File(filesDir, "dbip-country.mmdb")

    private fun repository(month: YearMonth) = GeoIpDatabaseRepositoryImpl(
        database = GeoIpDatabase(databaseFile()),
        preferencesDataSource = preferences,
        networkPolicy = FakeNetworkPolicy(),
        cacheDir = cacheDir,
        currentMonth = { month },
        urlForMonth = { "http://127.0.0.1:${server.address.port}/${GeoIpUrl.stamp(it)}" },
    )

    @Test
    fun `downloads and installs the current month`() = runTest {
        published["2026-07"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))

        repository(YearMonth.of(2026, 7)).refresh()

        assertTrue(databaseFile().exists())
        assertEquals(fixtureBytes("GeoIP2-Country-Test.mmdb").size.toLong(), databaseFile().length())
        assertEquals("2026-07", preferences.getString(PreferenceKeys.GEOIP_DB_MONTH, ""))
    }

    @Test
    fun `a new month replaces the database rather than adding a second file`() = runTest {
        // July's database is already installed.
        published["2026-07"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))
        repository(YearMonth.of(2026, 7)).refresh()
        assertEquals(listOf("dbip-country.mmdb"), filesDir.list()!!.sorted())

        // August publishes a differently sized database; force past the 30-day throttle.
        published["2026-08"] = gzip(fixtureBytes("GeoLite2-Country-Test.mmdb"))
        repository(YearMonth.of(2026, 8)).refresh(force = true)

        assertEquals(
            "the new month must overwrite, not accumulate",
            listOf("dbip-country.mmdb"),
            filesDir.list()!!.sorted(),
        )
        assertEquals(
            "the file must hold August's bytes",
            fixtureBytes("GeoLite2-Country-Test.mmdb").size.toLong(),
            databaseFile().length(),
        )
        assertEquals("2026-08", preferences.getString(PreferenceKeys.GEOIP_DB_MONTH, ""))
    }

    @Test
    fun `many refreshes over many months leave exactly one database and no scratch files`() = runTest {
        val months = listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9))
        months.forEachIndexed { index, month ->
            published[GeoIpUrl.stamp(month)] =
                gzip(fixtureBytes(if (index % 2 == 0) "GeoIP2-Country-Test.mmdb" else "GeoLite2-Country-Test.mmdb"))
            repository(month).refresh(force = true)
            assertEquals(listOf("dbip-country.mmdb"), filesDir.list()!!.sorted())
        }
        assertEquals("no leftover scratch files", emptyList<String>(), cacheDir.list()!!.sorted())
    }

    @Test
    fun `the scratch file is removed after a successful install`() = runTest {
        published["2026-07"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))
        val repository = repository(YearMonth.of(2026, 7))

        repository.refresh()

        assertFalse(repository.tempFile().exists())
        assertEquals(emptyList<String>(), cacheDir.list()!!.sorted())
    }

    @Test
    fun `a scratch file stranded by an earlier kill is reused, not accumulated`() = runTest {
        // Simulates a process killed mid-download: the temp survived with partial bytes.
        val stranded = File(cacheDir, "dbip-country.mmdb.tmp")
        stranded.writeBytes(ByteArray(4096) { 0x7F })
        published["2026-07"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))

        repository(YearMonth.of(2026, 7)).refresh()

        // The fixed temp path means the next download overwrites the corpse instead of
        // stranding another one beside it.
        assertEquals(emptyList<String>(), cacheDir.list()!!.sorted())
        assertEquals(listOf("dbip-country.mmdb"), filesDir.list()!!.sorted())
    }

    @Test
    fun `an unpublished month falls back to the previous one and still installs a single file`() = runTest {
        // Only June exists; July has not been published yet.
        published["2026-06"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))

        repository(YearMonth.of(2026, 7)).refresh()

        assertEquals(listOf("dbip-country.mmdb"), filesDir.list()!!.sorted())
        assertEquals("2026-06", preferences.getString(PreferenceKeys.GEOIP_DB_MONTH, ""))
        assertEquals(emptyList<String>(), cacheDir.list()!!.sorted())
    }

    @Test
    fun `a rejected payload leaves no database and no scratch file`() = runTest {
        published["2026-07"] = gzip("<html>404 not found</html>".toByteArray())
        published["2026-06"] = gzip("<html>404 not found</html>".toByteArray())

        repository(YearMonth.of(2026, 7)).refresh()

        assertFalse(databaseFile().exists())
        assertEquals(emptyList<String>(), cacheDir.list()!!.sorted())
    }

    @Test
    fun `deleteDatabase also removes a stranded scratch file`() = runTest {
        published["2026-07"] = gzip(fixtureBytes("GeoIP2-Country-Test.mmdb"))
        val repository = repository(YearMonth.of(2026, 7))
        repository.refresh()
        repository.tempFile().writeBytes(ByteArray(1024))

        repository.deleteDatabase()

        assertEquals(emptyList<String>(), filesDir.list()!!.sorted())
        assertEquals(emptyList<String>(), cacheDir.list()!!.sorted())
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

    private class FakeNetworkPolicy : NetworkPolicy {
        override val isWaitingForWifi: StateFlow<Boolean> = MutableStateFlow(false)
        override fun apply() = Unit
    }
}
