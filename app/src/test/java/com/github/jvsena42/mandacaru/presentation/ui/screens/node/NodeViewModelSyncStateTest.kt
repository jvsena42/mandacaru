package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.tor.TorProxyProbe
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.SyncPhase
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
import com.github.jvsena42.mandacaru.domain.floresta.phase
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.Result as BlockchainInfo

/**
 * Covers the P2 half of jvsena42/mandacaru#103: a rescan armed by a descriptor
 * load isn't fired until the chain has been at the tip for a grace window, and
 * until it has, the screen must not report being fully synced.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeViewModelSyncStateTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var rpc: FakeFlorestaRpc
    private lateinit var preferences: FakePreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc()
        preferences = FakePreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a pending rescan keeps the screen out of the synced state`() {
        preferences.setBooleanNow(PreferenceKeys.WALLET_NEEDS_RESCAN, true)
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))

        val state = buildViewModelAndPoll()

        assertTrue(state.walletRescanPending)
        assertFalse(state.rescanInProgress)
        assertEquals(SyncPhase.WALLET_SCAN, state.toSyncSnapshot().phase())
        assertFalse(
            computeSyncStepStates(
                phase = state.toSyncSnapshot().phase(),
                syncDecimal = state.syncDecimal,
                filterSyncDecimal = state.filterSyncDecimal,
            ).allDone
        )
    }

    @Test
    fun `the same state without a pending rescan is fully synced`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))

        val state = buildViewModelAndPoll()

        assertFalse(state.walletRescanPending)
        assertEquals(SyncPhase.SYNCED, state.toSyncSnapshot().phase())
        assertTrue(
            computeSyncStepStates(
                phase = state.toSyncSnapshot().phase(),
                syncDecimal = state.syncDecimal,
                filterSyncDecimal = state.filterSyncDecimal,
            ).allDone
        )
    }

    @Test
    fun `an unreachable Tor proxy is surfaced with its address`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))
        preferences.setBooleanNow(PreferenceKeys.TOR_ENABLED, true)

        val state = buildViewModelAndPoll(torProxyProbe = { false })

        assertEquals("127.0.0.1:9050", state.unreachableTorProxy)
    }

    @Test
    fun `a node still running without the saved Tor proxy asks for a restart`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))
        preferences.setBooleanNow(PreferenceKeys.TOR_ENABLED, true)

        val state = buildViewModelAndPoll(daemon = FakeFlorestaDaemon(running = true, proxy = null))

        assertTrue(state.torSettingPendingRestart)
    }

    @Test
    fun `a node still proxied after Tor was turned off asks for a restart`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))

        val state = buildViewModelAndPoll(
            daemon = FakeFlorestaDaemon(running = true, proxy = "127.0.0.1:9050"),
        )

        assertTrue(state.torSettingPendingRestart)
    }

    @Test
    fun `a node running with the saved Tor proxy needs no restart`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))
        preferences.setBooleanNow(PreferenceKeys.TOR_ENABLED, true)

        val state = buildViewModelAndPoll(
            daemon = FakeFlorestaDaemon(running = true, proxy = "127.0.0.1:9050"),
        )

        assertFalse(state.torSettingPendingRestart)
    }

    @Test
    fun `the Tor proxy is not probed while Tor is off`() {
        rpc.blockchainInfoResults = listOf(Result.success(syncedResponse()))
        var probed = false

        val state = buildViewModelAndPoll(torProxyProbe = { probed = true; false })

        assertFalse(probed)
        assertNull(state.unreachableTorProxy)
    }

    private fun buildViewModelAndPoll(
        torProxyProbe: TorProxyProbe = TorProxyProbe { true },
        daemon: FlorestaDaemon = FakeFlorestaDaemon(),
    ): NodeUiState {
        val vm = NodeViewModel(
            florestaRpc = rpc,
            snapshotService = FakeSnapshotService(FakeFlorestaDaemon()),
            florestaDaemon = daemon,
            preferencesDataSource = preferences,
            networkPolicyManager = FakeNetworkPolicy(),
            peerCountryLookup = { null },
            torProxyProbe = torProxyProbe,
            ioDispatcher = dispatcher,
        )
        dispatcher.scheduler.runCurrent()
        return vm.uiState.value
    }

    private fun syncedResponse() = GetBlockchainInfoResponse(
        id = 1,
        jsonrpc = "2.0",
        result = BlockchainInfo(
            bestBlock = "00".repeat(32),
            chain = "main",
            difficulty = 1f,
            height = 947_390,
            ibd = false,
            latestBlockTime = 0,
            latestWork = "0",
            leafCount = 0,
            rootCount = 0,
            rootHashes = emptyList(),
            validated = 947_390,
            filters = 947_390,
            rescanInProgress = false,
        ),
    )

    // --- fakes ---

    private class FakePreferences : PreferencesDataSource {
        private val strings = mutableMapOf<PreferenceKeys, String>()
        private val booleans = mutableMapOf<PreferenceKeys, Boolean>()
        fun setBooleanNow(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun setString(key: PreferenceKeys, value: String) { strings[key] = value }
        override suspend fun getString(key: PreferenceKeys, defaultValue: String): String =
            strings[key] ?: defaultValue
        override suspend fun setBoolean(key: PreferenceKeys, value: Boolean) { booleans[key] = value }
        override suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean): Boolean =
            booleans[key] ?: defaultValue
    }

    private class FakeSnapshotService(daemon: FlorestaDaemon) : UtreexoSnapshotService(daemon)

    private class FakeFlorestaDaemon(
        private val running: Boolean = false,
        private val proxy: String? = null,
    ) : FlorestaDaemon {
        override suspend fun start() = Unit
        override suspend fun stop() = Unit
        override fun isRunning(): Boolean = running
        override fun runningProxy(): String? = proxy
        override fun rpcCookieFile(): java.io.File? = null
        override suspend fun dumpUtreexoState(): Result<String> = Result.success("")
        override suspend fun prepareForSnapshotImport(): Result<Unit> = Result.success(Unit)
    }

    private class FakeNetworkPolicy : NetworkPolicy {
        override val isWaitingForWifi: StateFlow<Boolean> = MutableStateFlow(false)
        override fun apply() = Unit
    }
}
