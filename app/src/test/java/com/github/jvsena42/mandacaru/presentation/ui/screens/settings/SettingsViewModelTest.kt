package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import android.content.Context
import com.github.jvsena42.mandacaru.data.AppUpdateRepository
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.GeoIpDatabaseRepository
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.UpdateStatus
import com.github.jvsena42.mandacaru.domain.scan.DescriptorQrScanner
import com.github.jvsena42.mandacaru.domain.scan.DescriptorScanState
import com.github.jvsena42.mandacaru.fakes.FakeFlorestaRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Covers the copy-descriptor-to-clipboard flow added in #113: tapping a loaded descriptor
 * surfaces the "copied" confirmation, and clearing it resets the message. The clipboard
 * write itself lives in the Composable; the ViewModel only owns the confirmation message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var rpc: FakeFlorestaRpc
    private lateinit var preferences: FakePreferences
    private lateinit var appUpdateRepository: FakeAppUpdateRepository
    private lateinit var descriptorScanner: FakeDescriptorQrScanner

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc()
        preferences = FakePreferences()
        appUpdateRepository = FakeAppUpdateRepository()
        descriptorScanner = FakeDescriptorQrScanner()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SettingsViewModel {
        val vm = SettingsViewModel(
            florestaRpc = rpc,
            preferencesDataSource = preferences,
            appUpdateRepository = appUpdateRepository,
            geoIpDatabaseRepository = FakeGeoIpDatabaseRepository(),
            descriptorScanner = descriptorScanner,
            context = mock(Context::class.java),
        )
        // runCurrent (not advanceUntilIdle): observeRescanState is an infinite delay loop
        // that would never let advanceUntilIdle return; runCurrent parks it at the first delay.
        dispatcher.scheduler.runCurrent()
        return vm
    }

    @Test
    fun `copying a descriptor surfaces the copied confirmation`() {
        val vm = buildViewModel()

        vm.onAction(SettingsAction.OnDescriptorCopied(DESCRIPTOR))
        dispatcher.scheduler.runCurrent()

        assertEquals("Descriptor copied to clipboard", vm.uiState.value.snackBarMessage)
    }

    @Test
    fun `clearing the snackbar resets the message`() {
        val vm = buildViewModel()
        vm.onAction(SettingsAction.OnDescriptorCopied(DESCRIPTOR))
        dispatcher.scheduler.runCurrent()

        vm.onAction(SettingsAction.ClearSnackBarMessage)
        dispatcher.scheduler.runCurrent()

        assertEquals("", vm.uiState.value.snackBarMessage)
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

    private class FakeAppUpdateRepository : AppUpdateRepository {
        override val updateStatus: StateFlow<UpdateStatus> = MutableStateFlow(UpdateStatus())
        override suspend fun refresh(force: Boolean) = Unit
        override suspend fun markUpdateSeen() = Unit
    }

    private class FakeDescriptorQrScanner : DescriptorQrScanner {
        override fun ingest(raw: String): DescriptorScanState = DescriptorScanState.Idle
        override fun reset() = Unit
    }

    private companion object {
        const val DESCRIPTOR =
            "wpkh([73c5da0a/84h/1h/0h]tpubDC8msFGeGuwnKG9Upg7DM2b4DaRqg3CUZa5g8v2SRQ6K4NSkxUgd7HsL2XVWbVm39yBA4LgAFKvDsdsBPzMw3RGYbjeMs9dGcTLeUw6f7c/0/*)"
    }

    private class FakeGeoIpDatabaseRepository : GeoIpDatabaseRepository {
        override suspend fun refresh(force: Boolean) = Unit
        override suspend fun deleteDatabase() = Unit
    }
}
