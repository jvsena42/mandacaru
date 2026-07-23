package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.florestad.UtreexoImportException
import com.github.jvsena42.mandacaru.common.runSuspendCatching
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.floresta.toFlorestaNetwork
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
import com.github.jvsena42.mandacaru.domain.floresta.computeFilterSyncDecimal
import com.github.jvsena42.mandacaru.domain.floresta.computeHeaderSyncProgress
import com.github.jvsena42.mandacaru.domain.floresta.computeRescanProgressDecimal
import com.github.jvsena42.mandacaru.domain.floresta.hasUtreexoServiceFlag
import com.github.jvsena42.mandacaru.domain.floresta.isLikelyStalled
import com.github.jvsena42.mandacaru.domain.geoip.PeerCountryLookup
import com.github.jvsena42.mandacaru.domain.settings.isAdvancedFeaturesEnabled
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import com.github.jvsena42.mandacaru.presentation.utils.HexUtils
import com.github.jvsena42.mandacaru.presentation.utils.SnapshotCodec
import com.github.jvsena42.mandacaru.presentation.utils.countryCodeToFlagEmoji
import com.github.jvsena42.mandacaru.presentation.utils.toHumanReadableDifficulty
import com.github.jvsena42.mandacaru.presentation.utils.toSyncPercentageString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds
import com.florestad.Network as FlorestaNetwork

@Suppress("TooManyFunctions", "LongParameterList")
class NodeViewModel(
    private val florestaRpc: FlorestaRpc,
    private val snapshotService: UtreexoSnapshotService,
    private val florestaDaemon: FlorestaDaemon,
    private val preferencesDataSource: PreferencesDataSource,
    private val networkPolicyManager: NetworkPolicy,
    private val peerCountryLookup: PeerCountryLookup,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), EventFlow<NodeEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getInLoop()
        viewModelScope.launch {
            networkPolicyManager.isWaitingForWifi.collect { waiting ->
                _uiState.update { it.copy(isWaitingForWifi = waiting) }
            }
        }
    }

    private fun getInLoop() {
        viewModelScope.launch(ioDispatcher) {
            refreshPreferences()
            getInfo()
            delay(10.seconds)
            getInLoop()
        }
    }

    private suspend fun refreshPreferences() {
        val enabled = preferencesDataSource.isAdvancedFeaturesEnabled()
        // A rescan armed by a descriptor load or birthday change only fires
        // once the chain has been at the tip for a grace window. Until it
        // does, the wallet still has history to find and we must not claim
        // to be fully synced.
        val rescanPending = preferencesDataSource
            .getBoolean(PreferenceKeys.WALLET_NEEDS_RESCAN, false)
        _uiState.update {
            it.copy(enableAdvancedFeatures = enabled, walletRescanPending = rescanPending)
        }
    }

    private fun getInfo() {
        viewModelScope.launch(ioDispatcher) {
            florestaRpc.getBlockchainInfo().collect { result ->
                result.onSuccess { data ->
                    val filterHeight = data.result.filters
                    val filterDecimal = computeFilterSyncDecimal(
                        filters = filterHeight,
                        filtersStart = data.result.filtersStart,
                        height = data.result.height,
                    )
                    val rescanTotal = data.result.rescanBlocksTotal ?: 0
                    val rescanProcessed = data.result.rescanBlocksProcessed ?: 0
                    val rescanDecimal = computeRescanProgressDecimal(
                        processed = rescanProcessed,
                        total = rescanTotal,
                    )
                    _uiState.update {
                        it.copy(
                            blockHeight = NumberFormat.getNumberInstance().format(data.result.height),
                            headerHeightRaw = data.result.height,
                            difficulty = data.result.difficulty.toHumanReadableDifficulty(),
                            network = data.result.chain.uppercase(),
                            blockHash = data.result.bestBlock,
                            syncPercentage = data.result.progress.toSyncPercentageString(),
                            syncDecimal = data.result.progress,
                            validatedBLocks = data.result.validated,
                            ibd = data.result.ibd,
                            rescanInProgress = data.result.rescanInProgress,
                            rescanProgressDecimal = rescanDecimal,
                            rescanProgressPercentage = rescanDecimal
                                ?.toSyncPercentageString() ?: "0.00",
                            rescanBlocksProcessed = rescanProcessed,
                            rescanBlocksTotal = rescanTotal,
                            filterHeightRaw = filterHeight ?: 0,
                            filterSyncDecimal = filterDecimal,
                            filterSyncPercentage = filterDecimal
                                ?.toSyncPercentageString() ?: "0.00",
                        )
                    }
                    if (!data.result.ibd) {
                        clearPendingSnapshotIfAny()
                    }
                }
                updatePeerInfo()
                updateDiagnostics()
            }
        }
    }

    fun togglePeersExpanded() {
        _uiState.update { it.copy(isPeersExpanded = !it.isPeersExpanded) }
    }

    fun toggleDiagnosticsExpanded() {
        _uiState.update { it.copy(isDiagnosticsExpanded = !it.isDiagnosticsExpanded) }
    }

    private suspend fun updateDiagnostics() {
        if (!_uiState.value.enableAdvancedFeatures) return
        florestaRpc.getUptime().collect { result ->
            result.onSuccess { data ->
                _uiState.update { it.copy(uptime = formatUptime(data.result)) }
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val days = seconds / SECONDS_PER_DAY
        val hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR
        val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }
    }

    fun disconnectPeer(address: String) {
        viewModelScope.launch(ioDispatcher) {
            florestaRpc.disconnectNode(address).collect { result ->
                result.onSuccess { updatePeerInfo() }
                result.onFailure { e ->
                    Log.e(TAG, "disconnectPeer failure: ${e.message}")
                }
            }
        }
    }

    fun pingPeers() {
        viewModelScope.launch(ioDispatcher) {
            florestaRpc.ping().collect { result ->
                result.onFailure { e -> Log.e(TAG, "ping failure: ${e.message}") }
            }
        }
    }

    private suspend fun updatePeerInfo() {
        florestaRpc.getPeerInfo().collect { result ->
            result.onSuccess { data ->
                val peers = data.result.orEmpty()
                // Resolved out here: the update block below is not a suspend context.
                val peerUis = peers.map { peer ->
                    val countryCode = peerCountryLookup.countryCode(peer.address)
                    PeerUi(
                        peer = peer,
                        countryCode = countryCode,
                        flag = countryCode?.let(::countryCodeToFlagEmoji),
                    )
                }
                _uiState.update { current ->
                    val isHeaderSync = current.ibd && current.syncDecimal == 0f
                    val decimal = if (isHeaderSync) {
                        computeHeaderSyncProgress(current.headerHeightRaw, peers)
                    } else {
                        null
                    }
                    val stalled = isLikelyStalled(
                        progress = current.syncDecimal,
                        ibd = current.ibd,
                        ourHeight = current.headerHeightRaw,
                        peers = peers,
                    )
                    current.copy(
                        numberOfPeers = peers.size.toString(),
                        peers = peerUis,
                        utreexoPeerCount = peers.count { p -> p.services.hasUtreexoServiceFlag() },
                        headerSyncDecimal = decimal,
                        headerSyncPercentage = decimal?.toSyncPercentageString() ?: "0.00",
                        isStalled = stalled,
                    )
                }
            }
        }
    }

    fun onClickScan() {
        if (!_uiState.value.ibd) return
        _uiState.update { it.copy(isScanSheetOpen = true) }
    }

    fun onClickPaste() {
        if (!_uiState.value.ibd) return
        _uiState.update {
            it.copy(isPasteSheetOpen = true, pasteSheetText = "", pasteSheetError = null)
        }
    }

    fun onDismissScanSheet() {
        _uiState.update { it.copy(isScanSheetOpen = false) }
    }

    fun onDismissPasteSheet() {
        _uiState.update {
            it.copy(isPasteSheetOpen = false, pasteSheetText = "", pasteSheetError = null)
        }
    }

    fun onPasteSheetTextChanged(text: String) {
        _uiState.update { it.copy(pasteSheetText = text, pasteSheetError = null) }
    }

    fun onClickPasteFromClipboard(clip: String?) {
        if (!_uiState.value.ibd) return
        val text = clip?.trim().orEmpty()
        if (text.isEmpty()) {
            _uiState.update { it.copy(pasteSheetError = CLIPBOARD_INVALID_MESSAGE) }
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val currentNetworkEnum = currentNetwork()
            snapshotService.validate(text, currentNetworkEnum)
                .onSuccess {
                    _uiState.update { it.copy(pasteSheetText = text, pasteSheetError = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(pasteSheetError = errorToMessage(error, currentNetworkEnum))
                    }
                }
        }
    }

    fun onCheckClipboardForImport(clip: String?) {
        if (!_uiState.value.ibd) return
        val text = clip?.trim().orEmpty()
        if (text.isEmpty()) return
        viewModelScope.launch(ioDispatcher) {
            snapshotService.validate(text, currentNetwork()).onSuccess {
                val preview = snapshotService.peek(text).getOrNull() ?: return@onSuccess
                if (preview.height > _uiState.value.validatedBLocks) {
                    _uiState.update { it.copy(clipboardImportPayload = text) }
                }
            }
        }
    }

    fun onAcceptClipboardHint() {
        val payload = _uiState.value.clipboardImportPayload ?: return
        if (!_uiState.value.ibd) return
        _uiState.update { it.copy(clipboardImportPayload = null) }
        onAccumulatorReceived(payload)
    }

    fun onDismissClipboardHint() {
        _uiState.update { it.copy(clipboardImportPayload = null) }
    }

    fun onAccumulatorReceived(payload: String) {
        if (!_uiState.value.ibd) return
        _uiState.update {
            it.copy(
                isScanSheetOpen = false,
                isPasteSheetOpen = false,
                pasteSheetText = "",
                pasteSheetError = null,
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val currentNetworkEnum = currentNetwork()
            snapshotService.validate(payload, currentNetworkEnum).onFailure { error ->
                _uiState.update {
                    it.copy(snapshotMessage = errorToMessage(error, currentNetworkEnum))
                }
                return@launch
            }
            val preview = snapshotService.peek(payload)
            preview.onFailure {
                _uiState.update { it.copy(snapshotMessage = "Unrecognised snapshot format.") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    pendingSnapshotPreview = preview.getOrNull(),
                    pendingSnapshotPayload = payload,
                )
            }
        }
    }

    fun onDismissImportConfirm() {
        _uiState.update {
            it.copy(pendingSnapshotPreview = null, pendingSnapshotPayload = null)
        }
    }

    fun onConfirmImport() {
        val payload = _uiState.value.pendingSnapshotPayload ?: return
        if (!_uiState.value.ibd) return
        _uiState.update {
            it.copy(
                isApplyingSnapshot = true,
                pendingSnapshotPreview = null,
                pendingSnapshotPayload = null,
            )
        }
        viewModelScope.launch(ioDispatcher) {
            Log.i(
                TAG,
                "onConfirmImport: payload len=${payload.length} " +
                    "compact=${SnapshotCodec.isCompact(payload)}",
            )
            val payloadToPersist = runSuspendCatching { SnapshotCodec.normalizeToJson(payload) }
                .getOrElse { error ->
                    Log.e(TAG, "normalizeToJson failed", error)
                    _uiState.update {
                        it.copy(
                            isApplyingSnapshot = false,
                            snapshotMessage = "Snapshot payload was unreadable.",
                        )
                    }
                    return@launch
                }
            persistAndVerifyPendingSnapshot(payloadToPersist)
            florestaDaemon.stop()
            florestaDaemon.prepareForSnapshotImport()
                .onFailure { error ->
                    Log.e(TAG, "prepareForSnapshotImport failed", error)
                    _uiState.update {
                        it.copy(
                            isApplyingSnapshot = false,
                            snapshotMessage = "Failed to prepare for import: ${error.message}",
                        )
                    }
                    return@launch
                }
            Log.i(TAG, "onConfirmImport: OK — restart pending")
            with(viewModelScope) { sendEvent(NodeEvents.OnSnapshotApplied) }
        }
    }

    private suspend fun persistAndVerifyPendingSnapshot(payloadToPersist: String) {
        Log.i(
            TAG,
            "onConfirmImport: normalized JSON len=${payloadToPersist.length} " +
                "digest=${sha256Short(payloadToPersist)}",
        )
        preferencesDataSource.setString(
            PreferenceKeys.PENDING_UTREEXO_SNAPSHOT,
            payloadToPersist,
        )
        val readBack = preferencesDataSource
            .getString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
        if (readBack.length != payloadToPersist.length) {
            Log.e(
                TAG,
                "onConfirmImport: DataStore read-back MISMATCH " +
                    "wrote=${payloadToPersist.length} read=${readBack.length}",
            )
        } else {
            Log.i(TAG, "onConfirmImport: setString OK, read-back len=${readBack.length}")
        }
    }

    fun toggleImportCardExpanded() {
        if (!_uiState.value.ibd) return
        _uiState.update { it.copy(isImportCardExpanded = !it.isImportCardExpanded) }
    }

    fun toggleExportCardExpanded() {
        if (_uiState.value.ibd) return
        _uiState.update { it.copy(isExportCardExpanded = !it.isExportCardExpanded) }
    }

    fun onClickShowExportQr() = withExportPayload { payload ->
        _uiState.update { it.copy(isExportQrSheetOpen = true, exportPayload = payload) }
    }

    fun onClickCopyExport() = withExportPayload { payload ->
        _uiState.update {
            it.copy(exportPayload = payload, snapshotMessage = COPIED_MESSAGE)
        }
    }

    fun onClickShareExport() = withExportPayload { payload ->
        _uiState.update { it.copy(exportPayload = payload) }
        with(viewModelScope) { sendEvent(NodeEvents.OnShareAccumulator(payload)) }
    }

    fun onDismissExportQrSheet() {
        _uiState.update { it.copy(isExportQrSheetOpen = false) }
    }

    fun clearSnapshotMessage() {
        _uiState.update { it.copy(snapshotMessage = null) }
    }

    private fun withExportPayload(then: (String) -> Unit) {
        if (_uiState.value.ibd) return
        val cached = _uiState.value.exportPayload
        if (cached != null) {
            then(cached)
            return
        }
        viewModelScope.launch(ioDispatcher) {
            snapshotService.dump()
                .onSuccess { then(it) }
                .onFailure { error ->
                    Log.w(TAG, "dumpUtreexoState failed", error)
                    _uiState.update {
                        it.copy(snapshotMessage = "Could not export snapshot: ${error.message}")
                    }
                }
        }
    }

    private suspend fun currentNetwork(): FlorestaNetwork {
        return preferencesDataSource.getString(
            PreferenceKeys.CURRENT_NETWORK,
            FlorestaNetwork.BITCOIN.name,
        ).toFlorestaNetwork()
    }

    private suspend fun clearPendingSnapshotIfAny() {
        val existing = preferencesDataSource
            .getString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
        if (existing.isNotEmpty()) {
            Log.i(TAG, "clearPendingSnapshotIfAny: clearing ${existing.length}-char snapshot (ibd=false)")
            preferencesDataSource.setString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
        }
    }

    private fun sha256Short(s: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        val head = HexUtils.bytesToHex(digest.copyOfRange(0, DIGEST_PREFIX_BYTES))
        val tail = HexUtils.bytesToHex(digest.copyOfRange(digest.size - DIGEST_PREFIX_BYTES, digest.size))
        return "$head..$tail"
    }

    private fun errorToMessage(error: Throwable, currentNetwork: FlorestaNetwork): String =
        when (error) {
            is UtreexoImportException.NetworkMismatch ->
                "This snapshot is for a different network than this node ($currentNetwork)."
            is UtreexoImportException.UnsupportedVersion ->
                "This snapshot uses a newer format than this app supports."
            is UtreexoImportException.InvalidHex ->
                "Snapshot contains malformed data."
            is UtreexoImportException.UnknownNetwork,
            is UtreexoImportException.InvalidJson ->
                "Unrecognised snapshot format."
            else -> error.message ?: "Snapshot import failed."
        }

    private companion object {
        const val TAG = "NodeViewModel"
        const val SECONDS_PER_DAY = 86400L
        const val SECONDS_PER_HOUR = 3600L
        const val SECONDS_PER_MINUTE = 60L
        const val COPIED_MESSAGE = "Copied to clipboard"
        const val CLIPBOARD_INVALID_MESSAGE = "Clipboard does not contain a valid accumulator."
        const val DIGEST_PREFIX_BYTES = 4
    }
}
