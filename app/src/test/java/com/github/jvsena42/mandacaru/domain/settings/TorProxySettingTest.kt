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
    fun `no route while Tor is off`() = runTest {
        val prefs = MapPreferences(strings = mapOf(PreferenceKeys.CUSTOM_TOR_PROXY to "10.0.0.2:9050"))
        assertEquals(TorMode.OFF, prefs.getTorMode())
        assertNull(prefs.torRoute())
    }

    @Test
    fun `Tor switched on before modes existed routes everything`() = runTest {
        val prefs = MapPreferences(booleans = mapOf(PreferenceKeys.TOR_ENABLED to true))
        assertEquals(TorRoute(TorMode.ALL, DEFAULT_TOR_PROXY), prefs.torRoute())
    }

    @Test
    fun `a saved mode wins over the old switch`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(PreferenceKeys.TOR_MODE to TorMode.ONION_PEERS.name),
            booleans = mapOf(PreferenceKeys.TOR_ENABLED to true),
        )
        assertEquals(TorMode.ONION_PEERS, prefs.getTorMode())
    }

    @Test
    fun `Tor turned on by an older build after saving Off routes everything`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(PreferenceKeys.TOR_MODE to TorMode.OFF.name),
            booleans = mapOf(PreferenceKeys.TOR_ENABLED to true),
        )
        assertEquals(TorMode.ALL, prefs.getTorMode())
    }

    @Test
    fun `an unknown saved mode fails closed to everything over Tor`() = runTest {
        val prefs = MapPreferences(strings = mapOf(PreferenceKeys.TOR_MODE to "SOMETHING"))
        assertEquals(TorMode.ALL, prefs.getTorMode())
    }

    @Test
    fun `custom proxy is used by the route`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(
                PreferenceKeys.CUSTOM_TOR_PROXY to "10.0.0.2:9150",
                PreferenceKeys.TOR_MODE to TorMode.ONION_PEERS.name,
            ),
        )
        assertEquals(
            TorRoute(TorMode.ONION_PEERS, TorProxyAddress("10.0.0.2", 9150)),
            prefs.torRoute(),
        )
    }

    @Test
    fun `a corrupt saved proxy keeps Tor on through the default`() = runTest {
        val prefs = MapPreferences(
            strings = mapOf(
                PreferenceKeys.CUSTOM_TOR_PROXY to "garbage",
                PreferenceKeys.TOR_MODE to TorMode.ALL.name,
            ),
        )
        assertEquals(TorRoute(TorMode.ALL, DEFAULT_TOR_PROXY), prefs.torRoute())
    }

    @Test
    fun `each mode fills exactly one of the node's proxies`() {
        val onionPeers = TorRoute(TorMode.ONION_PEERS, DEFAULT_TOR_PROXY)
        assertNull(onionPeers.allPeersProxy)
        assertEquals("127.0.0.1:9050", onionPeers.onionPeersProxy)

        val all = TorRoute(TorMode.ALL, DEFAULT_TOR_PROXY)
        assertEquals("127.0.0.1:9050", all.allPeersProxy)
        assertNull(all.onionPeersProxy)
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
