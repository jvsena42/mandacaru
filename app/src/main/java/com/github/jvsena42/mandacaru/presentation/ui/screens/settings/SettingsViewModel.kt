package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import com.github.jvsena42.mandacaru.presentation.utils.getElectrumPort
import com.github.jvsena42.mandacaru.presentation.utils.getNetwork
import com.github.jvsena42.mandacaru.presentation.utils.getRpcPort
import com.github.jvsena42.mandacaru.presentation.utils.removeSpaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import com.florestad.Network as FlorestaNetwork


class SettingsViewModel(
    private val florestaRpc: FlorestaRpc,
    private val preferencesDataSource: PreferencesDataSource
) : ViewModel(), EventFlow<SettingsEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedNetwork = preferencesDataSource.getString(
                        PreferenceKeys.CURRENT_NETWORK,
                        FlorestaNetwork.BITCOIN.name
                    ),
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
        if (_uiState.value.nodeAddress.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.addNode(_uiState.value.nodeAddress)
                .collect { result ->
                    result.onSuccess { data ->
                        _uiState.update { it.copy(nodeAddress = "", snackBarMessage = "Node connected successfully") }
                        Log.d(TAG, "connectNode: Success: $data")
                    }.onFailure { error ->
                        Log.d(TAG, "connectNode: Fail: ${error.message}")
                        _uiState.update { it.copy(snackBarMessage = error.message.toString()) }
                    }

                    delay(2.seconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
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

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}