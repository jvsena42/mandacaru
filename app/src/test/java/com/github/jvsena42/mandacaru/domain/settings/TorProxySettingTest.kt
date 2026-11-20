package com.github.jvsena42.mandacaru.domain.settings

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TorProxySettingTest {

    @Test
    fun `empty input means the Orbot default`() {
        assertEquals(TorProxyInput.Default, parseTorProxy("  "))
    }

    @Test
    fun `the Orbot default is not stored as custom`() {
        assertEquals(TorProxyInput.Default, parseTorProxy("127.0.0.1:9050"))
    }

    @Test
    fun `accepts another IPv4 proxy`() {
        assertEquals(
            TorProxyInput.Custom(TorProxyAddress("192.168.1.10", 9150)),
            parseTorProxy(" 192.168.1.10:9150 "),
        )
    }

    @Test
    fun `strips leading zeros so the node never takes the hostname path`() {
        assertEquals(
            TorProxyInput.Custom(TorProxyAddress("10.0.0.1", 9050)),
            parseTorProxy("010.000.000.001:9050"),
        )
        assertEquals(TorProxyInput.Default, parseTorProxy("127.000.000.001:09050"))
    }

    @Test
    fun `rejects inputs the node cannot use without a resolver`() {
        listOf(
            "localhost:9050",
            "127.0.0.1",
            "127.0.0.1:",
            ":9050",
            "[::1]:9050",
            "127.0.0.1:9050:1",
            "127.0.0:9050",
            "127.0.0.256:9050",
            "127.0.0.1:0",
            "127.0.0.1:65536",
            "127.0.0.1:+905",
            "1270.0.0.1:9050",
        ).forEach { assertEquals(it, TorProxyInput.Invalid, parseTorProxy(it)) }
    }

    @Test
    fun `no proxy while Tor is off`() = runTest {
        val prefs = MapPreferences(strings = mapOf(PreferenceKeys.CUSTOM_TOR_PROXY to "10.0.0.2:9050"))
        assertNull(prefs.torProxyAddress())
    }

    @Test
    fun `default proxy when Tor is on without a custom one`() = runTest {
        val prefs = MapPreferences(booleans = mapOf(PreferenceKeys.TOR_ENABLED to true))
        assertEquals(DEFAULT_TOR_PROXY, prefs.torProxyAddress())
    }

    @Test
    fun `custom proxy when Tor is on`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(PreferenceKeys.CUSTOM_TOR_PROXY to "10.0.0.2:9150"),
            booleans = mapOf(PreferenceKeys.TOR_ENABLED to true),
        )
        assertEquals(TorProxyAddress("10.0.0.2", 9150), prefs.torProxyAddress())
    }

    @Test
    fun `a corrupt saved proxy keeps Tor on through the default`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(PreferenceKeys.CUSTOM_TOR_PROXY to "garbage"),
            booleans = mapOf(PreferenceKeys.TOR_ENABLED to true),
        )
        assertEquals(DEFAULT_TOR_PROXY, prefs.torProxyAddress())
    }

    private class MapPreferences(
        strings: Map<PreferenceKeys, String> = emptyMap(),
        booleans: Map<PreferenceKeys, Boolean> = emptyMap(),
    ) : PreferencesDataSource {
        private val strings = strings.toMutableMap()
        private val booleans = booleans.toMutableMap()
        override suspend fun setString(key: PreferenceKeys, value: String) { strings[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String) =
            strings[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean) =
            booleans[key] ?: defaultValue
    }
}
