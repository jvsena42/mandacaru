package com.github.jvsena42.mandacaru.domain.settings

import com.florestad.Network
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ElectrumPortSettingTest {

    @Test
    fun `empty input means the network default`() {
        assertEquals(ElectrumPortInput.Default, parseElectrumPort("  ", Network.BITCOIN))
    }

    @Test
    fun `the network default port is not stored as custom`() {
        assertEquals(ElectrumPortInput.Default, parseElectrumPort("50001", Network.BITCOIN))
    }

    @Test
    fun `accepts an unprivileged port`() {
        assertEquals(ElectrumPortInput.Custom(50002), parseElectrumPort("50002", Network.BITCOIN))
    }

    @Test
    fun `rejects privileged and out of range ports`() {
        assertEquals(ElectrumPortInput.OutOfRange, parseElectrumPort("1023", Network.BITCOIN))
        assertEquals(ElectrumPortInput.OutOfRange, parseElectrumPort("65536", Network.BITCOIN))
        assertEquals(ElectrumPortInput.OutOfRange, parseElectrumPort("abc", Network.BITCOIN))
    }

    @Test
    fun `rejects the rpc port of the selected network`() {
        assertEquals(ElectrumPortInput.ConflictsWithRpc, parseElectrumPort("8332", Network.BITCOIN))
        assertEquals(ElectrumPortInput.ConflictsWithRpc, parseElectrumPort("38332", Network.SIGNET))
    }

    @Test
    fun `server address marks plaintext tcp`() {
        assertEquals("127.0.0.1:50001:t", electrumServerAddress("50001"))
    }

    @Test
    fun `accepts the port range boundaries and leading zeros`() {
        assertEquals(ElectrumPortInput.Custom(1024), parseElectrumPort("1024", Network.BITCOIN))
        assertEquals(ElectrumPortInput.Custom(65535), parseElectrumPort("65535", Network.BITCOIN))
        assertEquals(ElectrumPortInput.Custom(50002), parseElectrumPort("050002", Network.BITCOIN))
        assertEquals(ElectrumPortInput.OutOfRange, parseElectrumPort("99999999999", Network.BITCOIN))
    }

    @Test
    fun `the default is per network`() {
        assertEquals(ElectrumPortInput.Default, parseElectrumPort("60001", Network.SIGNET))
    }

    @Test
    fun `effective port honours a custom port`() = runTest {
        val prefs = MapPreferences(PreferenceKeys.CUSTOM_ELECTRUM_PORT to "50002")
        assertEquals("50002", prefs.electrumPortFor(Network.BITCOIN))
    }

    @Test
    fun `effective port falls back to the default when the custom one collides with rpc`() = runTest {
        val prefs = MapPreferences(PreferenceKeys.CUSTOM_ELECTRUM_PORT to "38332")
        assertEquals("60001", prefs.electrumPortFor(Network.SIGNET))
    }

    @Test
    fun `effective port falls back to the default on a garbage value`() = runTest {
        val prefs = MapPreferences(PreferenceKeys.CUSTOM_ELECTRUM_PORT to "abc")
        assertEquals("50001", prefs.electrumPortFor(Network.BITCOIN))
    }

    private class MapPreferences(vararg entries: Pair<PreferenceKeys, String>) : PreferencesDataSource {
        private val strings = mutableMapOf(*entries)
        override suspend fun setString(key: PreferenceKeys, value: String) { strings[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String) =
            strings[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) = Unit
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean) = defaultValue
    }
}
