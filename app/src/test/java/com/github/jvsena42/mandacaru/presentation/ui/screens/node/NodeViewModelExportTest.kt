package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.mandacaru.fakes.FakeFlorestaRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.Result as BlockchainInfo

/**
 * The "Share validation" card exports the node's Utreexo accumulator. It used
 * to keep the first dump and hand it out for the rest of the session, so a
 * payload copied hours later described a state dozens of blocks behind.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeViewModelExportTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var rpc: FakeFlorestaRpc
    private lateinit var snapshotService: FakeSnapshotService

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc().apply {
            blockchainInfoResults = listOf(Result.success(syncedResponse()))
        }
        snapshotService = FakeSnapshotService(FakeFlorestaDaemon())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `copy hands out the accumulator of the moment, not the first one dumped`() {
        val vm = buildViewModel()
        val copied = collectCopies(vm)

        snapshotService.nextDump = "accumulator-at-967818"
        vm.onClickCopyExport()
        dispatcher.scheduler.runCurrent()

        snapshotService.nextDump = "accumulator-at-967875"
        vm.onClickCopyExport()
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("accumulator-at-967818", "accumulator-at-967875"), copied)
        assertEquals("accumulator-at-967875", vm.uiState.value.exportPayload)
    }

    @Test
    fun `the first copy already carries a payload`() {
        val vm = buildViewModel()
        val copied = collectCopies(vm)

        snapshotService.nextDump = "accumulator"
        vm.onClickCopyExport()
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("accumulator"), copied)
        assertEquals("Copied to clipboard", vm.uiState.value.snapshotMessage)
    }

    @Test
    fun `the QR sheet shows a fresh dump each time it is opened`() {
        val vm = buildViewModel()

        snapshotService.nextDump = "first"
        vm.onClickShowExportQr()
        dispatcher.scheduler.runCurrent()
        vm.onDismissExportQrSheet()

        snapshotService.nextDump = "second"
        vm.onClickShowExportQr()
        dispatcher.scheduler.runCurrent()

        assertEquals("second", vm.uiState.value.exportPayload)
    }

    private fun collectCopies(vm: NodeViewModel): List<String> {
        val copied = mutableListOf<String>()
        TestScope(dispatcher).launch {
            vm.eventFlow.collect { event ->
                if (event is NodeEvents.OnCopyAccumulator) copied += event.payload
            }
        }
        dispatcher.scheduler.runCurrent()
        return copied
    }

    private fun buildViewModel(): NodeViewModel {
        val vm = NodeViewModel(
            florestaRpc = rpc,
            snapshotService = snapshotService,
            florestaDaemon = FakeFlorestaDaemon(),
            preferencesDataSource = FakePreferences(),
            networkPolicyManager = FakeNetworkPolicy(),
            peerCountryLookup = { null },
            ioDispatcher = dispatcher,
        )
        dispatcher.scheduler.runCurrent()
        return vm
    }

    private fun syncedResponse() = GetBlockchainInfoResponse(
        id = 1,
        jsonrpc = "2.0",
        result = BlockchainInfo(
            bestBlock = "00".repeat(32),
            chain = "main",
            difficulty = 1f,
            height = 967_875,
            ibd = false,
            latestBlockTime = 0,
            latestWork = "0",
            leafCount = 0,
            rootCount = 0,
            rootHashes = emptyList(),
            validated = 967_875,
            filters = 967_875,
            rescanInProgress = false,
        ),
    )

    private class FakeSnapshotService(daemon: FlorestaDaemon) : UtreexoSnapshotService(daemon) {
        var nextDump = ""
        override suspend fun dump(): Result<String> = Result.success(nextDump)
    }

    private class FakeFlorestaDaemon : FlorestaDaemon {
        override suspend fun start() = Unit
        override suspend fun stop() = Unit
        override fun isRunning(): Boolean = false
        override suspend fun dumpUtreexoState(): Result<String> = Result.success("")
        override suspend fun prepareForSnapshotImport(): Result<Unit> = Result.success(Unit)
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
