package com.github.jvsena42.mandacaru.data.geoip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxmind.db.Reader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetAddress

/**
 * Exercises the maxmind-db calls [GeoIpDatabase] relies on, on ART rather than the host JVM.
 * Since 4.x the library ships Java 17 bytecode and decodes its metadata into a record through
 * reflection, so a JVM-only unit test cannot catch a bump that breaks the reader on device.
 */
@RunWith(AndroidJUnit4::class)
class MaxMindReaderInstrumentedTest {

    private lateinit var database: File

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        database = File(instrumentation.targetContext.cacheDir, "maxmind-reader-test.mmdb")
        instrumentation.context.assets.open(FIXTURE).use { input ->
            database.outputStream().use { input.copyTo(it) }
        }
    }

    @After
    fun tearDown() {
        database.delete()
    }

    @Test
    fun metadata_exposesCountryDatabaseType() {
        Reader(database).use { reader ->
            assertTrue(reader.metadata.databaseType.contains("country", ignoreCase = true))
        }
    }

    @Test
    fun memoryMappedReader_resolvesCountryIsoCodes() {
        Reader(database, Reader.FileMode.MEMORY_MAPPED).use { reader ->
            assertEquals("GB", reader.isoCode("81.2.69.160"))
            assertEquals("RO", reader.isoCode("2a02:d800::1"))
            assertNull(reader.isoCode("127.0.0.1"))
        }
    }

    private fun Reader.isoCode(address: String): String? =
        (get(InetAddress.getByName(address), Map::class.java)?.get("country") as? Map<*, *>)
            ?.get("iso_code") as? String

    private companion object {
        const val FIXTURE = "geoip/GeoIP2-Country-Test.mmdb"
    }
}
