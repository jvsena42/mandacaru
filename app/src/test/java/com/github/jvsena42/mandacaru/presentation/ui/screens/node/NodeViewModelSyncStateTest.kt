package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
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

    private fun buildViewModelAndPoll(): NodeUiState {
        val vm = NodeViewModel(
            florestaRpc = rpc,
            snapshotService = FakeSnapshotService(FakeFlorestaDaemon()),
            florestaDaemon = FakeFlorestaDaemon(),
            preferencesDataSource = preferences,
            networkPolicyManager = FakeNetworkPolicy(),
            peerCountryLookup = { null },
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
            chain = "bitcoin",
            difficulty = 1f,
            height = 947_390,
            ibd = false,
            latestBlockTime = 0,
            latestWork = "0",
            leafCount = 0,
            progress = 1f,
            rootCount = 0,
            rootHashes = emptyList(),
            validated = 947_390,
            filters = 947_390,
            filtersStart = 0,
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

    private class FakeFlorestaDaemon : FlorestaDaemon {
        override suspend fun start() = Unit
        override suspend fun stop() = Unit
        override fun isRunning(): Boolean = false
        override suspend fun dumpUtreexoState(): Result<String> = Result.success("")
        override suspend fun prepareForSnapshotImport(): Result<Unit> = Result.success(Unit)
    }

    private class FakeNetworkPolicy : NetworkPolicy {
        override val isWaitingForWifi: StateFlow<Boolean> = MutableStateFlow(false)
        override fun apply() = Unit
    }
}
