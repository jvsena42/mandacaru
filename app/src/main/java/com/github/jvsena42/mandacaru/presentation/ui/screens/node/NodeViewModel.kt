package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.florestad.UtreexoImportException
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.floresta.toFlorestaNetwork
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
import com.github.jvsena42.mandacaru.domain.floresta.hasUtreexoServiceFlag
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import com.github.jvsena42.mandacaru.presentation.utils.toHumanReadableDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds
import com.florestad.Network as FlorestaNetwork

class NodeViewModel(
    private val florestaRpc: FlorestaRpc,
    private val snapshotService: UtreexoSnapshotService,
    private val florestaDaemon: FlorestaDaemon,
    private val preferencesDataSource: PreferencesDataSource,
) : ViewModel(), EventFlow<NodeEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getInLoop()
    }

    private fun getInLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            getInfo()
            delay(10.seconds)
            getInLoop()
        }
    }

    private fun getInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.getBlockchainInfo().collect { result ->
                result.onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            blockHeight = NumberFormat.getNumberInstance().format(data.result.height),
                            difficulty = data.result.difficulty.toHumanReadableDifficulty(),
                            network = data.result.chain.uppercase(),
                            blockHash = data.result.bestBlock,
                            syncPercentage = "%.2f".format(data.result.progress * PERCENTAGE_MULTIPLIER),
                            syncDecimal = data.result.progress,
                            validatedBLocks = data.result.validated,
                            ibd = data.result.ibd
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
        florestaRpc.getUptime().collect { result ->
            result.onSuccess { data ->
                _uiState.update { it.copy(uptime = formatUptime(data.result)) }
            }
        }
        florestaRpc.getMemoryInfo().collect { result ->
            result.onSuccess { data ->
                val locked = data.result.locked
                _uiState.update {
                    it.copy(
                        memoryUsed = formatBytes(locked.used),
                        memoryFree = formatBytes(locked.free),
                        memoryTotal = formatBytes(locked.total),
                    )
                }
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val days = seconds / SECONDS_PER_DAY
        val hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR
        val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${secs}s")
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= BYTES_PER_MB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
            bytes >= BYTES_PER_KB -> "%.1f KB".format(bytes / BYTES_PER_KB.toDouble())
            else -> "$bytes B"
        }
    }

    fun disconnectPeer(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.disconnectNode(address).collect { result ->
                result.onSuccess { updatePeerInfo() }
                result.onFailure { e ->
                    Log.e(TAG, "disconnectPeer failure: ${e.message}")
                }
            }
        }
    }

    fun pingPeers() {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.ping().collect { result ->
                result.onFailure { e -> Log.e(TAG, "ping failure: ${e.message}") }
            }
        }
    }

    private suspend fun updatePeerInfo() {
        florestaRpc.getPeerInfo().collect { result ->
            result.onSuccess { data ->
                val peers = data.result.orEmpty()
                _uiState.update {
                    it.copy(
                        numberOfPeers = peers.size.toString(),
                        peers = peers,
                        utreexoPeerCount = peers.count { p -> p.services.hasUtreexoServiceFlag() },
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
        _uiState.update { it.copy(isPasteSheetOpen = true) }
    }

    fun onDismissScanSheet() {
        _uiState.update { it.copy(isScanSheetOpen = false) }
    }

    fun onDismissPasteSheet() {
        _uiState.update { it.copy(isPasteSheetOpen = false) }
    }

    fun onAccumulatorReceived(payload: String) {
        if (!_uiState.value.ibd) return
        _uiState.update {
            it.copy(isScanSheetOpen = false, isPasteSheetOpen = false)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            preferencesDataSource.setString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, payload)
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
            with(viewModelScope) { sendEvent(NodeEvents.OnSnapshotApplied) }
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
        viewModelScope.launch(Dispatchers.IO) {
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
            preferencesDataSource.setString(PreferenceKeys.PENDING_UTREEXO_SNAPSHOT, "")
        }
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
        const val PERCENTAGE_MULTIPLIER = 100
        const val SECONDS_PER_DAY = 86400L
        const val SECONDS_PER_HOUR = 3600L
        const val SECONDS_PER_MINUTE = 60L
        const val BYTES_PER_KB = 1_024L
        const val BYTES_PER_MB = 1_048_576L
        const val COPIED_MESSAGE = "Copied to clipboard"
    }
}
