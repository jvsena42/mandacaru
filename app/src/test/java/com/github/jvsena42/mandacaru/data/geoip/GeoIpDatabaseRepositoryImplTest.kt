package com.github.jvsena42.mandacaru.data.geoip

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GeoIpDatabaseRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val preferences = FakePreferences()

    private fun fixture(): File = File(
        checkNotNull(javaClass.getResource("/geoip/GeoIP2-Country-Test.mmdb")).toURI()
    )

    private fun repository(databaseFile: File) = GeoIpDatabaseRepositoryImpl(
        database = GeoIpDatabase(databaseFile),
        preferencesDataSource = preferences,
        networkPolicy = FakeNetworkPolicy(),
        cacheDir = temporaryFolder.newFolder(),
    )

    @Test
    fun `deleteDatabase removes the file`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        fixture().copyTo(target, overwrite = true)

        repository(target).deleteDatabase()

        assertFalse(target.exists())
    }

    @Test
    fun `deleteDatabase clears the stamp and the throttle so re-enabling downloads at once`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        fixture().copyTo(target, overwrite = true)
        preferences.setString(PreferenceKeys.GEOIP_DB_MONTH, "2026-07")
        preferences.setString(PreferenceKeys.GEOIP_LAST_CHECK, System.currentTimeMillis().toString())

        repository(target).deleteDatabase()

        // Leaving either of these set would strand a re-enabling user without flags for up to
        // 30 days, since refresh() would consider the check already done.
        assertEquals("", preferences.getString(PreferenceKeys.GEOIP_DB_MONTH, ""))
        assertEquals("", preferences.getString(PreferenceKeys.GEOIP_LAST_CHECK, ""))
    }

    @Test
    fun `refresh does nothing while the feature is disabled`() = runTest {
        val target = File(temporaryFolder.root, "live.mmdb")
        preferences.setBoolean(PreferenceKeys.GEOIP_FLAGS_ENABLED, false)

        // Would otherwise reach the network; the absence of a download is the assertion.
        repository(target).refresh(force = true)

        assertFalse(target.exists())
        assertEquals("", preferences.getString(PreferenceKeys.GEOIP_LAST_CHECK, ""))
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
