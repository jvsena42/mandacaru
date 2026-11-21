package com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor

import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.common.CoroutineDispatchers
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.settings.DEFAULT_TOR_PROXY
import com.github.jvsena42.mandacaru.domain.settings.TorMode
import com.github.jvsena42.mandacaru.domain.settings.TorProxyAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TorSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val preferences = MapPreferences()
    private val probed = mutableListOf<TorProxyAddress>()
    private var proxyReachable = true

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = TorSettingsViewModel(
        preferencesDataSource = preferences,
        torProxyProbe = { probed += it; proxyReachable },
        dispatchers = CoroutineDispatchers(io = dispatcher, default = dispatcher),
    ).also { dispatcher.scheduler.runCurrent() }

    @Test
    fun `enabling Tor with a reachable proxy saves it and restarts`() = runTest(dispatcher) {
        val vm = buildViewModel()
        var restarted = false
        val collector = launch { vm.eventFlow.first(); restarted = true }

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.ALL))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf(DEFAULT_TOR_PROXY), probed)
        assertEquals(TorMode.ALL.name, preferences.strings[PreferenceKeys.TOR_MODE])
        assertEquals(true, preferences.booleans[PreferenceKeys.TOR_ENABLED])
        assertEquals("", preferences.strings[PreferenceKeys.CUSTOM_TOR_PROXY])
        assertTrue(restarted)
    }

    @Test
    fun `enabling Tor with an unreachable proxy shows an error and saves nothing`() = runTest(dispatcher) {
        proxyReachable = false
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.ALL))
        vm.onAction(TorSettingsAction.OnProxyChanged("10.0.0.2:9150"))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertEquals(listOf(TorProxyAddress("10.0.0.2", 9150)), probed)
        assertEquals(R.string.tor_proxy_error_unreachable, vm.uiState.value.proxyError)
        assertFalse(vm.uiState.value.isApplying)
        assertNull(preferences.strings[PreferenceKeys.TOR_MODE])
    }

    @Test
    fun `an invalid proxy is rejected before probing`() = runTest(dispatcher) {
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.ALL))
        vm.onAction(TorSettingsAction.OnProxyChanged("localhost:9050"))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertEquals(R.string.tor_proxy_error_invalid, vm.uiState.value.proxyError)
        assertTrue(probed.isEmpty())
        assertNull(preferences.strings[PreferenceKeys.TOR_MODE])
    }

    @Test
    fun `onion peers mode is probed and saved like everything over Tor`() = runTest(dispatcher) {
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.ONION_PEERS))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertEquals(listOf(DEFAULT_TOR_PROXY), probed)
        assertEquals(TorMode.ONION_PEERS.name, preferences.strings[PreferenceKeys.TOR_MODE])
        assertEquals(true, preferences.booleans[PreferenceKeys.TOR_ENABLED])
    }

    @Test
    fun `switching between Tor modes is a change`() = runTest(dispatcher) {
        preferences.strings[PreferenceKeys.TOR_MODE] = TorMode.ALL.name
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.ONION_PEERS))

        assertTrue(vm.uiState.value.hasChanges)
    }

    @Test
    fun `disabling Tor never waits on the proxy`() = runTest(dispatcher) {
        proxyReachable = false
        preferences.booleans[PreferenceKeys.TOR_ENABLED] = true
        val vm = buildViewModel()
        assertEquals(TorMode.ALL, vm.uiState.value.savedMode)

        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.OFF))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertTrue(probed.isEmpty())
        assertEquals(TorMode.OFF.name, preferences.strings[PreferenceKeys.TOR_MODE])
        assertEquals(false, preferences.booleans[PreferenceKeys.TOR_ENABLED])
    }

    @Test
    fun `disabling Tor keeps the saved proxy instead of an unprobed edit`() = runTest(dispatcher) {
        preferences.booleans[PreferenceKeys.TOR_ENABLED] = true
        preferences.strings[PreferenceKeys.CUSTOM_TOR_PROXY] = "10.0.0.2:9150"
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnProxyChanged("10.0.0.3:9150"))
        vm.onAction(TorSettingsAction.OnModeSelected(TorMode.OFF))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertEquals("10.0.0.2:9150", preferences.strings[PreferenceKeys.CUSTOM_TOR_PROXY])
        assertEquals(TorMode.OFF.name, preferences.strings[PreferenceKeys.TOR_MODE])
        assertEquals(false, preferences.booleans[PreferenceKeys.TOR_ENABLED])
    }

    @Test
    fun `retyping the saved proxy in another form is not a change`() = runTest(dispatcher) {
        preferences.booleans[PreferenceKeys.TOR_ENABLED] = true
        val vm = buildViewModel()

        vm.onAction(TorSettingsAction.OnProxyChanged("127.0.0.1:9050"))
        vm.onAction(TorSettingsAction.OnClickApply)
        advanceUntilIdle()

        assertTrue(probed.isEmpty())
        assertEquals("", vm.uiState.value.proxyText)
        assertFalse(vm.uiState.value.isApplying)
    }

    private class MapPreferences : PreferencesDataSource {
        val strings = mutableMapOf<PreferenceKeys, String>()
        val booleans = mutableMapOf<PreferenceKeys, Boolean>()
        override suspend fun setString(key: PreferenceKeys, value: String) { strings[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String) =
            strings[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean) =
            booleans[key] ?: defaultValue
    }
}
