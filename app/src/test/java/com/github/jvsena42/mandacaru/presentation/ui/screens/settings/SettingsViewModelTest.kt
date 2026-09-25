package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import android.content.Context
import com.github.jvsena42.mandacaru.common.CoroutineDispatchers
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.WalletDescriptorStatus
import com.github.jvsena42.mandacaru.domain.scan.DescriptorQrScanner
import com.github.jvsena42.mandacaru.domain.scan.DescriptorScanState
import com.github.jvsena42.mandacaru.fakes.FakeAppUpdateRepository
import com.github.jvsena42.mandacaru.fakes.FakeFlorestaRpc
import com.github.jvsena42.mandacaru.fakes.FakeGeoIpDatabaseRepository
import com.github.jvsena42.mandacaru.fakes.FakeWalletDescriptorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    private lateinit var walletDescriptorRepository: FakeWalletDescriptorRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc()
        preferences = FakePreferences()
        appUpdateRepository = FakeAppUpdateRepository()
        descriptorScanner = FakeDescriptorQrScanner()
        walletDescriptorRepository = FakeWalletDescriptorRepository()
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
            walletDescriptorRepository = walletDescriptorRepository,
            context = mock(Context::class.java),
            dispatchers = CoroutineDispatchers(io = dispatcher, default = dispatcher),
        )
        // runCurrent (not advanceUntilIdle): observeRescanState is an infinite delay loop
        // that would never let advanceUntilIdle return; runCurrent parks it at the first delay.
        dispatcher.scheduler.runCurrent()
        return vm
    }

    @Test
    fun `sharing a descriptor opens the share sheet and dismissing closes it`() {
        val vm = buildViewModel()

        vm.onAction(SettingsAction.OnClickShareDescriptor(DESCRIPTOR))
        dispatcher.scheduler.runCurrent()
        assertEquals(DESCRIPTOR, vm.uiState.value.descriptorToShare)

        vm.onAction(SettingsAction.OnDismissDescriptorShareSheet)
        dispatcher.scheduler.runCurrent()
        assertEquals(null, vm.uiState.value.descriptorToShare)
    }

    @Test
    fun `clearing the snackbar resets the message`() {
        val vm = buildViewModel()
        // Seed a message synchronously via the private-key rejection path.
        vm.onAction(SettingsAction.OnDescriptorChanged("xprv9s21ZrQH143K3GJpoapnV8SFfuZcECeGTqTKP9HYSnmgYfq6"))
        vm.onAction(SettingsAction.OnClickUpdateDescriptor)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.snackBarMessage.isNotEmpty())

        vm.onAction(SettingsAction.ClearSnackBarMessage)
        dispatcher.scheduler.runCurrent()

        assertEquals("", vm.uiState.value.snackBarMessage)
    }

    @Test
    fun `descriptors come from the repository`() {
        val vm = buildViewModel()

        walletDescriptorRepository.status.value =
            WalletDescriptorStatus(descriptors = listOf(DESCRIPTOR), isKnown = true)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(DESCRIPTOR), vm.uiState.value.descriptors)
    }

    @Test
    fun `loading a descriptor refreshes the repository so the prompts clear immediately`() =
        runTest(dispatcher) {
            rpc.loadDescriptorResult = Result.success(JSONObject())
            val vm = buildViewModel()

            vm.onAction(SettingsAction.OnDescriptorChanged(DESCRIPTOR))
            vm.onAction(SettingsAction.OnClickUpdateDescriptor)
            runCurrent()

            assertEquals(1, walletDescriptorRepository.refreshCount)
            assertEquals(listOf(DESCRIPTOR), rpc.loadedDescriptors)
            assertTrue(vm.uiState.value.isLoading)

            // The loading state is held for a moment after the answer
            advanceTimeBy(LOADING_HOLD_MS)
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)

            // observeRescanState polls forever; runTest would otherwise wait for it
            vm.viewModelScope.cancel()
        }

    @Test
    fun `typed descriptor text loses any whitespace`() {
        val vm = buildViewModel()

        vm.onAction(SettingsAction.OnDescriptorChanged(" $ZPUB\n"))
        dispatcher.scheduler.runCurrent()

        assertEquals(ZPUB, vm.uiState.value.descriptorText)
    }

    @Test
    fun `a corrupted extended key is rejected before reaching the node`() = runTest(dispatcher) {
        val vm = buildViewModel()

        vm.onAction(SettingsAction.OnDescriptorChanged(ZPUB.dropLast(1)))
        vm.onAction(SettingsAction.OnClickUpdateDescriptor)
        runCurrent()

        assertTrue(vm.uiState.value.snackBarMessage.contains("checksum"))
        assertEquals(emptyList<String>(), rpc.loadedDescriptors)
        assertFalse(vm.uiState.value.isLoading)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `expanding descriptors is idempotent unlike toggling`() {
        val vm = buildViewModel()

        vm.onAction(SettingsAction.ExpandDescriptors)
        vm.onAction(SettingsAction.ExpandDescriptors)
        dispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isDescriptorsExpanded)
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

    private class FakeDescriptorQrScanner : DescriptorQrScanner {
        override fun ingest(raw: String): DescriptorScanState = DescriptorScanState.Idle
        override fun reset() = Unit
    }

    private companion object {
        const val LOADING_HOLD_MS = 2_001L
        const val DESCRIPTOR =
            "wpkh([73c5da0a/84h/1h/0h]tpubDC8msFGeGuwnKG9Upg7DM2b4DaRqg3CUZa5g8v2SRQ6K4NSkxUgd7HsL2XVWbVm39yBA4LgAFKvDsdsBPzMw3RGYbjeMs9dGcTLeUw6f7c/0/*)"
        const val ZPUB =
            "zpub6rFR7y4Q2AijBEqTUquhVz398htDFrtymD9xYYfG1m4wAcvPhXNfE3EfH1r1ADqtfSdVCToUG868RvUUkgDKf31mGDtKsAYz2oz2AGutZYs"
    }

}
