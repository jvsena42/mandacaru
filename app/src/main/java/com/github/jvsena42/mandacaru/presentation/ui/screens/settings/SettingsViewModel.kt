package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.AppUpdateRepository
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.AddNodeCommand
import com.github.jvsena42.mandacaru.domain.scan.DescriptorQrScanner
import com.github.jvsena42.mandacaru.domain.scan.DescriptorScanState
import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import com.github.jvsena42.mandacaru.presentation.utils.PeerAddressValidator
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday
import com.github.jvsena42.mandacaru.presentation.utils.getElectrumPort
import com.github.jvsena42.mandacaru.presentation.utils.getNetwork
import com.github.jvsena42.mandacaru.presentation.utils.getRpcPort
import com.github.jvsena42.mandacaru.presentation.utils.removeSpaces
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.florestad.Network as FlorestaNetwork


class SettingsViewModel(
    private val florestaRpc: FlorestaRpc,
    private val preferencesDataSource: PreferencesDataSource,
    private val appUpdateRepository: AppUpdateRepository,
    private val descriptorScanner: DescriptorQrScanner,
    @field:SuppressLint("StaticFieldLeak") private val context: Context,
) : ViewModel(), EventFlow<SettingsEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var nodeAddressValidationJob: Job? = null
    private var descriptorScanErrorJob: Job? = null
    private var rescanPollJob: Job? = null
    private var wasRescanning = false

    init {
        viewModelScope.launch {
            val birthdayYear = preferencesDataSource
                .getString(PreferenceKeys.WALLET_BIRTHDAY_YEAR, "")
                .toIntOrNull()
                ?: WalletBirthday.defaultYear()
            val useAlsoMobileData = preferencesDataSource
                .getBoolean(PreferenceKeys.USE_ALSO_MOBILE_DATA, false)
            _uiState.update {
                it.copy(
                    selectedNetwork = preferencesDataSource.getString(
                        PreferenceKeys.CURRENT_NETWORK,
                        FlorestaNetwork.BITCOIN.name
                    ),
                    walletBirthdayYear = birthdayYear,
                    useAlsoMobileData = useAlsoMobileData,
                )
            }
            updateElectrumAddress()
        }
        getDescriptors()
        observeUpdateStatus()
        observeRescanState()
    }

    /**
     * Polls the node for rescan state so the Rescan button reflects the real
     * background scan (driven by `rescan_in_progress` from `getblockchaininfo`)
     * instead of a fixed timer. Also surfaces a completion message and catches
     * rescans started elsewhere (e.g. the auto-rescan after a descriptor load).
     */
    private fun observeRescanState() {
        rescanPollJob?.cancel()
        rescanPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val info = florestaRpc.getBlockchainInfo().firstOrNull()?.getOrNull()?.result
                if (info != null) {
                    val running = info.rescanInProgress
                    _uiState.update {
                        it.copy(
                            isRescanning = running,
                            rescanBlocksProcessed = info.rescanBlocksProcessed,
                            rescanBlocksTotal = info.rescanBlocksTotal,
                        )
                    }
                    if (wasRescanning && !running) {
                        _uiState.update { it.copy(snackBarMessage = "Rescan complete") }
                    }
                    wasRescanning = running
                }
                delay(RESCAN_POLL_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun observeUpdateStatus() {
        viewModelScope.launch { appUpdateRepository.refresh(force = true) }
        viewModelScope.launch {
            appUpdateRepository.updateStatus.collect { status ->
                _uiState.update { it.copy(updateStatus = status) }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnDescriptorChanged -> {
                _uiState.update {
                    it.copy(descriptorText = action.descriptor.removeSpaces())
                }
            }

            is SettingsAction.OnClickUpdateDescriptor -> updateDescriptor()

            SettingsAction.OnClickScanDescriptor -> openDescriptorScanner()
            SettingsAction.OnDismissDescriptorScanner -> closeDescriptorScanner()
            is SettingsAction.OnDescriptorQrFrameScanned -> handleDescriptorFrame(action.raw)
            SettingsAction.OnConfirmScannedDescriptor -> confirmScannedDescriptor()
            SettingsAction.OnDismissScannedDescriptor -> _uiState.update {
                it.copy(pendingScannedDescriptor = null)
            }

            SettingsAction.OnClickRescan -> rescan()
            SettingsAction.ClearSnackBarMessage -> _uiState.update { it.copy(snackBarMessage = "") }
            SettingsAction.OnClickConnectNode -> connectNode()
            is SettingsAction.OnNodeAddressChanged -> {
                _uiState.update {
                    it.copy(
                        nodeAddress = action.address.removeSpaces(),
                        nodeAddressError = null,
                        isNodeAddressValid = false,
                    )
                }
                debouncedValidateNodeAddress()
            }

            is SettingsAction.OnNetworkSelected -> handleNetworkSelected(action)
            SettingsAction.ToggleDescriptorsExpanded -> _uiState.update {
                it.copy(
                    isDescriptorsExpanded = !it.isDescriptorsExpanded
                )
            }

            SettingsAction.ToggleNetworkExpanded -> _uiState.update {
                it.copy(isNetworkExpanded = !it.isNetworkExpanded)
            }

            SettingsAction.ToggleNodeExpanded -> _uiState.update {
                it.copy(isNodeExpanded = !it.isNodeExpanded)
            }

            SettingsAction.ToggleAboutExpanded -> toggleAboutExpanded()

            SettingsAction.OnClickGetUpdate -> getUpdate()

            SettingsAction.OnClickCheckForUpdates -> checkForUpdates()

            SettingsAction.ToggleDonateExpanded -> _uiState.update {
                it.copy(isDonateExpanded = !it.isDonateExpanded)
            }

            SettingsAction.OnClickExportLogs -> exportLogs()

            SettingsAction.ToggleBirthdayExpanded -> _uiState.update {
                it.copy(isBirthdayExpanded = !it.isBirthdayExpanded)
            }

            SettingsAction.OnClickChangeBirthdayYear -> _uiState.update {
                it.copy(isBirthdayPickerOpen = true)
            }

            SettingsAction.OnDismissBirthdayPicker -> _uiState.update {
                it.copy(isBirthdayPickerOpen = false)
            }

            is SettingsAction.OnBirthdayYearSelected -> _uiState.update {
                it.copy(isBirthdayPickerOpen = false, pendingBirthdayYear = action.year)
            }

            SettingsAction.OnCancelBirthdayRestart -> _uiState.update {
                it.copy(pendingBirthdayYear = null)
            }

            SettingsAction.OnConfirmBirthdayRestart -> applyBirthdayYearAndRestart()
            SettingsAction.ToggleDataUsageExpanded -> toggleDataUsageExpanded()
            is SettingsAction.OnToggleMobileData -> handleMobileDataToggled(action)
        }
    }

    private fun toggleDataUsageExpanded() = _uiState.update {
        it.copy(isDataUsageExpanded = !it.isDataUsageExpanded)
    }

    private fun handleMobileDataToggled(action: SettingsAction.OnToggleMobileData) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesDataSource.setBoolean(
                PreferenceKeys.USE_ALSO_MOBILE_DATA,
                action.enabled
            )
            _uiState.update { it.copy(useAlsoMobileData = action.enabled, isLoading = true) }
            delay(2.seconds)
            viewModelScope.sendEvent(SettingsEvents.OnNetworkPolicyChanged)
        }
    }

    private fun toggleAboutExpanded() {
        val willExpand = !_uiState.value.isAboutExpanded
        _uiState.update { it.copy(isAboutExpanded = willExpand) }
        if (willExpand) {
            viewModelScope.launch { appUpdateRepository.markUpdateSeen() }
        }
    }

    private fun getUpdate() {
        val status = _uiState.value.updateStatus
        viewModelScope.sendEvent(SettingsEvents.OpenReleasePage(status.releasePageUrl))
    }

    private fun checkForUpdates() {
        viewModelScope.launch { appUpdateRepository.refresh(force = true) }
    }

    private fun applyBirthdayYearAndRestart() {
        val year = _uiState.value.pendingBirthdayYear ?: return
        viewModelScope.launch(Dispatchers.IO) {
            preferencesDataSource.setString(
                PreferenceKeys.WALLET_BIRTHDAY_YEAR,
                year.toString()
            )
            // Floresta wipes the compact filter store and re-syncs from the new
            // height when this changes, but it does not auto-rescan loaded
            // descriptors against the new store. Without this flag the wallet
            // stays empty until the user manually re-loads the descriptor.
            preferencesDataSource.setBoolean(PreferenceKeys.WALLET_NEEDS_RESCAN, true)
            _uiState.update {
                it.copy(
                    walletBirthdayYear = year,
                    pendingBirthdayYear = null,
                    isLoading = true,
                )
            }
            delay(2.seconds)
            viewModelScope.sendEvent(SettingsEvents.OnBirthdayChanged)
        }
    }

    fun handleNetworkSelected(action: SettingsAction.OnNetworkSelected) {
        viewModelScope.launch(Dispatchers.IO) {
            //TODO MOVE TO A REPOSITORY
            preferencesDataSource.setString(PreferenceKeys.CURRENT_NETWORK, action.network)
            preferencesDataSource.setString(
                PreferenceKeys.CURRENT_RPC_PORT,
                action.network.getNetwork().getRpcPort()
            )
            _uiState.update { it.copy(selectedNetwork = action.network, isLoading = true) }
            updateElectrumAddress()
            delay(5.seconds)
            viewModelScope.sendEvent(SettingsEvents.OnNetworkChanged)
        }
    }

    private suspend fun updateElectrumAddress() {
        val network = preferencesDataSource.getString(
            PreferenceKeys.CURRENT_NETWORK,
            FlorestaNetwork.BITCOIN.name
        ).getNetwork()
        val port = network.getElectrumPort()
        _uiState.update { it.copy(electrumAddress = "127.0.0.1:$port") }
    }

    private fun getDescriptors() {
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.listDescriptors().collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "getDescriptors: $data")
                    _uiState.update { it.copy(descriptors = data.result) }
                }
            }
        }
    }

    private fun debouncedValidateNodeAddress() {
        nodeAddressValidationJob?.cancel()
        nodeAddressValidationJob = viewModelScope.launch {
            delay(VALIDATION_DEBOUNCE_MS.milliseconds)
            val address = _uiState.value.nodeAddress
            val result = PeerAddressValidator.validate(address)
            _uiState.update {
                when (result) {
                    PeerAddressValidator.Result.Valid -> it.copy(
                        isNodeAddressValid = true,
                        nodeAddressError = null,
                    )
                    PeerAddressValidator.Result.Empty -> it.copy(
                        isNodeAddressValid = false,
                        nodeAddressError = null,
                    )
                    PeerAddressValidator.Result.InvalidIpv4 -> it.copy(
                        isNodeAddressValid = false,
                        nodeAddressError = R.string.node_address_error_invalid_ipv4,
                    )
                    PeerAddressValidator.Result.InvalidIpv6 -> it.copy(
                        isNodeAddressValid = false,
                        nodeAddressError = R.string.node_address_error_invalid_ipv6,
                    )
                    PeerAddressValidator.Result.InvalidPort -> it.copy(
                        isNodeAddressValid = false,
                        nodeAddressError = R.string.node_address_error_invalid_port,
                    )
                    PeerAddressValidator.Result.InvalidFormat -> it.copy(
                        isNodeAddressValid = false,
                        nodeAddressError = R.string.node_address_error_invalid_format,
                    )
                }
            }
        }
    }

    private fun connectNode() {
        if (!_uiState.value.isNodeAddressValid) return
        val address = _uiState.value.nodeAddress
        if (address.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val onetryResult = florestaRpc.addNode(address, AddNodeCommand.ONETRY).firstOrNull()

            onetryResult?.onSuccess { data ->
                Log.d(TAG, "connectNode: onetry ok: $data")
                _uiState.update {
                    it.copy(
                        nodeAddress = "",
                        nodeAddressError = null,
                        isNodeAddressValid = false,
                        snackBarMessage = "Attempting connection to $address…"
                    )
                }
                florestaRpc.addNode(address, AddNodeCommand.ADD).firstOrNull()
                    ?.onFailure { Log.w(TAG, "connectNode: add (persist) failed: ${it.message}") }
            }?.onFailure { error ->
                Log.d(TAG, "connectNode: onetry failed: ${error.message}")
                _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
            }

            delay(2.seconds)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateDescriptor() {
        loadDescriptorString(_uiState.value.descriptorText) {
            _uiState.update { it.copy(descriptorText = "") }
        }
    }

    private fun loadDescriptorString(input: String, onSuccess: () -> Unit = {}) {
        if (DescriptorUtils.isPrivateKey(input)) {
            _uiState.update {
                it.copy(snackBarMessage = "Private keys are not supported. Please use a public key (xpub, zpub, etc.) or a full descriptor.")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.loadDescriptor(DescriptorUtils.wrapDescriptorIfNeeded(input))
                .collect { result ->
                    result.onSuccess { data ->
                        // The server-side rescan kicked off by loaddescriptor only
                        // covers filters already downloaded; flag a rescan so the
                        // service re-scans once filters reach the tip, picking up
                        // history in blocks downloaded after this point.
                        preferencesDataSource.setBoolean(PreferenceKeys.WALLET_NEEDS_RESCAN, true)
                        onSuccess()
                        _uiState.update { it.copy(snackBarMessage = "Descriptor loaded successfully") }
                        getDescriptors()
                        Log.d(TAG, "loadDescriptorString: Success: $data")
                    }.onFailure { error ->
                        Log.d(TAG, "loadDescriptorString: Fail: ${error.message}")
                        _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
                    }

                    delay(2.seconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun openDescriptorScanner() {
        descriptorScanErrorJob?.cancel()
        descriptorScanner.reset()
        _uiState.update {
            it.copy(
                isDescriptorScanSheetOpen = true,
                descriptorScanProgress = 0f,
                descriptorScanError = "",
                pendingScannedDescriptor = null,
            )
        }
    }

    private fun closeDescriptorScanner() {
        descriptorScanErrorJob?.cancel()
        descriptorScanner.reset()
        _uiState.update {
            it.copy(
                isDescriptorScanSheetOpen = false,
                descriptorScanProgress = 0f,
                descriptorScanError = "",
            )
        }
    }

    private fun handleDescriptorFrame(raw: String) {
        val current = _uiState.value
        if (current.pendingScannedDescriptor != null || current.descriptorScanError.isNotEmpty()) return
        when (val state = descriptorScanner.ingest(raw)) {
            is DescriptorScanState.Idle -> Unit
            is DescriptorScanState.InProgress -> _uiState.update {
                it.copy(descriptorScanProgress = state.progress, descriptorScanError = "")
            }
            is DescriptorScanState.Error -> showDescriptorScanError(state.reason)
            is DescriptorScanState.Complete -> onDescriptorScanned(state.descriptor)
        }
    }

    private fun onDescriptorScanned(descriptor: String) {
        val wrapped = DescriptorUtils.wrapDescriptorIfNeeded(descriptor)
        _uiState.update {
            it.copy(
                isDescriptorScanSheetOpen = false,
                descriptorScanProgress = 0f,
                descriptorScanError = "",
                pendingScannedDescriptor = PendingDescriptor(
                    descriptor = wrapped,
                    summary = DescriptorUtils.summarize(wrapped),
                ),
            )
        }
    }

    private fun confirmScannedDescriptor() {
        val pending = _uiState.value.pendingScannedDescriptor ?: return
        _uiState.update { it.copy(pendingScannedDescriptor = null) }
        loadDescriptorString(pending.descriptor)
    }

    private fun showDescriptorScanError(reason: String) {
        descriptorScanErrorJob?.cancel()
        _uiState.update { it.copy(descriptorScanError = reason, descriptorScanProgress = 0f) }
        descriptorScanErrorJob = viewModelScope.launch {
            delay(SCAN_ERROR_COOLDOWN_MS.milliseconds)
            descriptorScanner.reset()
            _uiState.update { it.copy(descriptorScanError = "", descriptorScanProgress = 0f) }
        }
    }

    private fun rescan() {
        if (_uiState.value.descriptors.isEmpty() || _uiState.value.isRescanning) return
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.rescan().collect { result ->
                result.onSuccess { data ->
                    // Optimistically reflect the running state; observeRescanState
                    // takes over from here (progress + completion message).
                    wasRescanning = true
                    _uiState.update {
                        it.copy(isRescanning = true, snackBarMessage = "Rescan started")
                    }
                    Log.d(TAG, "rescan: Success: $data")
                }.onFailure { error ->
                    Log.d(TAG, "rescan: Fail: ${error.message}")
                    _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
                }
            }
        }
    }

    private fun exportLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logFile = File(context.filesDir, "debug.log")
            if (!logFile.exists()) {
                _uiState.update { it.copy(snackBarMessage = "Log file not found") }
                return@launch
            }

            val cacheDir = File(context.cacheDir, "logs").apply { mkdirs() }
            val cachedLog = File(cacheDir, "debug.log")
            logFile.copyTo(cachedLog, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cachedLog
            )
            viewModelScope.sendEvent(SettingsEvents.OnExportLogs(uri))
        }
    }

    override fun onCleared() {
        super.onCleared()
        nodeAddressValidationJob?.cancel()
        descriptorScanErrorJob?.cancel()
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val VALIDATION_DEBOUNCE_MS = 500L
        private const val RESCAN_POLL_INTERVAL_MS = 3000L
        private const val SCAN_ERROR_COOLDOWN_MS = 10000L
    }
}
