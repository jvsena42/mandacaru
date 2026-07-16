package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import com.florestad.Network
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.SnapshotPreview
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the clipboard-aware Utreexo snapshot import flow added in #67:
 * the hint is only offered when the pasted snapshot is ahead of what we have
 * validated, and confirming an import normalizes + persists the payload and
 * prepares the daemon.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeViewModelClipboardImportTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var rpc: FakeFlorestaRpc
    private lateinit var daemon: FakeFlorestaDaemon
    private lateinit var snapshotService: FakeSnapshotService
    private lateinit var preferences: FakePreferences
    private lateinit var networkPolicy: FakeNetworkPolicy

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc()
        daemon = FakeFlorestaDaemon()
        snapshotService = FakeSnapshotService(daemon)
        preferences = FakePreferences()
        networkPolicy = FakeNetworkPolicy()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): NodeViewModel {
        val vm = NodeViewModel(
            florestaRpc = rpc,
            snapshotService = snapshotService,
            florestaDaemon = daemon,
            preferencesDataSource = preferences,
            networkPolicyManager = networkPolicy,
            peerCountryLookup = { null },
            ioDispatcher = dispatcher,
        )
        // Let the init poll settle to its first delay (RPC fakes emit nothing).
        dispatcher.scheduler.runCurrent()
        return vm
    }

    @Test
    fun `hint is offered when the snapshot is ahead of validated blocks`() {
        snapshotService.validateResult = Result.success(Unit)
        snapshotService.peekResult = Result.success(preview(height = 800_000L))
        val vm = buildViewModel()

        vm.onCheckClipboardForImport("snapshot-payload")
        dispatcher.scheduler.runCurrent()

        assertEquals("snapshot-payload", vm.uiState.value.clipboardImportPayload)
    }

    @Test
    fun `no hint when the clipboard snapshot fails validation`() {
        snapshotService.validateResult = Result.failure(IllegalStateException("bad"))
        val vm = buildViewModel()

        vm.onCheckClipboardForImport("snapshot-payload")
        dispatcher.scheduler.runCurrent()

        assertNull(vm.uiState.value.clipboardImportPayload)
    }

    @Test
    fun `confirming an import normalizes, persists and prepares the daemon`() {
        val payload = """{"version":1,"network":"signet","height":800000,"roots":[]}"""
        snapshotService.validateResult = Result.success(Unit)
        snapshotService.peekResult = Result.success(preview(height = 800_000L))
        val vm = buildViewModel()

        vm.onAccumulatorReceived(payload)
        dispatcher.scheduler.runCurrent()
        assertEquals(payload, vm.uiState.value.pendingSnapshotPayload)

        vm.onConfirmImport()
        dispatcher.scheduler.runCurrent()

        // A bare JSON payload normalizes to itself (only UTREEXO1… blobs are rewritten).
        assertEquals(payload, preferences.store[PreferenceKeys.PENDING_UTREEXO_SNAPSHOT])
        assertTrue("daemon should be stopped before import", daemon.stopped)
        assertTrue("daemon should be prepared for import", daemon.prepared)
        assertNull(vm.uiState.value.pendingSnapshotPayload)
    }

    private fun preview(height: Long) = SnapshotPreview(
        network = Network.SIGNET,
        height = height,
        blockHash = "00".repeat(32),
        rootCount = 0,
    )

    // --- fakes ---

    private class FakeSnapshotService(daemon: FlorestaDaemon) : UtreexoSnapshotService(daemon) {
        var validateResult: Result<Unit> = Result.success(Unit)
        var peekResult: Result<SnapshotPreview> = Result.failure(IllegalStateException("unset"))

        override suspend fun validate(payload: String, expectedNetwork: Network): Result<Unit> =
            validateResult

        override fun peek(payload: String): Result<SnapshotPreview> = peekResult
    }

    private class FakeFlorestaDaemon : FlorestaDaemon {
        var stopped = false
        var prepared = false
        override suspend fun start() = Unit
        override suspend fun stop() { stopped = true }
        override fun isRunning(): Boolean = false
        override suspend fun dumpUtreexoState(): Result<String> = Result.success("")
        override suspend fun prepareForSnapshotImport(): Result<Unit> {
            prepared = true
            return Result.success(Unit)
        }
    }

    private class FakePreferences : PreferencesDataSource {
        val store = mutableMapOf<PreferenceKeys, String>()
        private val booleans = mutableMapOf<PreferenceKeys, Boolean>()
        override suspend fun setString(key: PreferenceKeys, value: String) { store[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String): String =
            store[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean): Boolean =
            booleans[key] ?: defaultValue
    }

    private class FakeNetworkPolicy : NetworkPolicy {
        override val isWaitingForWifi: StateFlow<Boolean> = MutableStateFlow(false)
        override fun apply() = Unit
    }
}
