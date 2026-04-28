package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.AddNodeCommand
import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday
import com.github.jvsena42.mandacaru.presentation.utils.getElectrumPort
import com.github.jvsena42.mandacaru.presentation.utils.getNetwork
import com.github.jvsena42.mandacaru.presentation.utils.getRpcPort
import com.github.jvsena42.mandacaru.presentation.utils.removeSpaces
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import com.florestad.Network as FlorestaNetwork


class SettingsViewModel(
    private val florestaRpc: FlorestaRpc,
    private val preferencesDataSource: PreferencesDataSource,
    private val context: Context
) : ViewModel(), EventFlow<SettingsEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val birthdayYear = preferencesDataSource
                .getString(PreferenceKeys.WALLET_BIRTHDAY_YEAR, "")
                .toIntOrNull()
                ?: WalletBirthday.defaultYear()
            _uiState.update {
                it.copy(
                    selectedNetwork = preferencesDataSource.getString(
                        PreferenceKeys.CURRENT_NETWORK,
                        FlorestaNetwork.BITCOIN.name
                    ),
                    walletBirthdayYear = birthdayYear,
                )
            }
            updateElectrumAddress()
        }
        getDescriptors()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnDescriptorChanged -> {
                _uiState.update {
                    it.copy(descriptorText = action.descriptor.removeSpaces())
                }
            }

            is SettingsAction.OnClickUpdateDescriptor -> updateDescriptor()

            SettingsAction.OnClickRescan -> rescan()
            SettingsAction.ClearSnackBarMessage -> _uiState.update { it.copy(snackBarMessage = "") }
            SettingsAction.OnClickConnectNode -> connectNode()
            is SettingsAction.OnNodeAddressChanged -> _uiState.update {
                it.copy(nodeAddress = action.address.removeSpaces())
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

            SettingsAction.ToggleAboutExpanded -> _uiState.update {
                it.copy(isAboutExpanded = !it.isAboutExpanded)
            }

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
        }
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

    private fun connectNode() {
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
        val input = _uiState.value.descriptorText

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
                        _uiState.update { it.copy(descriptorText = "", snackBarMessage = "Descriptor loaded successfully") }
                        getDescriptors()
                        Log.d(TAG, "updateDescriptor: Success: $data")
                    }.onFailure { error ->
                        Log.d(TAG, "updateDescriptor: Fail: ${error.message}")
                        _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
                    }

                    delay(2.seconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun rescan() {
        if (_uiState.value.descriptors.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.rescan().collect { result ->
                result.onSuccess { data ->
                    _uiState.update { it.copy(snackBarMessage = "Rescan started") }
                    Log.d(TAG, "rescan: Success: $data")
                }.onFailure { error ->
                    Log.d(TAG, "rescan: Fail: ${error.message}")
                    _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
                }

                delay(2.seconds)
                _uiState.update { it.copy(isLoading = false) }
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

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
