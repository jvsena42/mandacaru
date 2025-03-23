package com.github.jvsena42.floresta_node.presentation.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.floresta_node.BuildConfig
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.model.Constants
import com.github.jvsena42.floresta_node.presentation.utils.EventFlow
import com.github.jvsena42.floresta_node.presentation.utils.EventFlowImpl
import com.github.jvsena42.floresta_node.presentation.utils.getNetwork
import com.github.jvsena42.floresta_node.presentation.utils.getRpcPort
import com.github.jvsena42.floresta_node.presentation.utils.removeSpaces
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
        _uiState.update {
            it.copy(
                selectedNetwork = preferencesDataSource.getString(
                    PreferenceKeys.CURRENT_NETWORK,
                    if (BuildConfig.DEBUG) FlorestaNetwork.SIGNET.name else FlorestaNetwork.BITCOIN.name
                ),
            )
        }
        updateElectrumAddress()
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
            SettingsAction.ClearSnackBarMessage -> _uiState.update { it.copy(errorMessage = "") }
            SettingsAction.OnClickConnectNode -> connectNode()
            is SettingsAction.OnNodeAddressChanged -> _uiState.update {
                it.copy(nodeAddress = action.address.removeSpaces())
            }

            is SettingsAction.OnNetworkSelected -> handleNetworkSelected(action)
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

    private fun updateElectrumAddress() {
        val port = preferencesDataSource.getString(
            PreferenceKeys.CURRENT_RPC_PORT,
            defaultValue = if (BuildConfig.DEBUG) Constants.RPC_PORT_SIGNET else Constants.RPC_PORT_MAINNET
        )
        _uiState.update { it.copy(electrumAddress = "127.0.0.1:$port") }
    }

    private fun connectNode() {
        if (_uiState.value.nodeAddress.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.addNode(_uiState.value.nodeAddress)
                .collect { result ->
                    result.onSuccess { data ->
                        _uiState.update { it.copy(nodeAddress = "") }
                        Log.d(TAG, "connectNode: Success: $data")
                    }.onFailure { error ->
                        Log.d(TAG, "connectNode: Fail: ${error.message}")
                        _uiState.update { it.copy(errorMessage = error.message.toString()) }
                    }

                    delay(2.seconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun updateDescriptor() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.loadDescriptor(_uiState.value.descriptorText)
                .collect { result ->
                    result.onSuccess { data ->
                        _uiState.update { it.copy(descriptorText = "") }
                        Log.d(TAG, "updateDescriptor: Success: $data")
                    }.onFailure { error ->
                        Log.d(TAG, "updateDescriptor: Fail: ${error.message}")
                        _uiState.update { it.copy(errorMessage = error.message.toString()) }
                    }

                    delay(2.seconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun rescan() {
        if (_uiState.value.descriptorText.removeSpaces().isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            florestaRpc.rescan().collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "rescan: Success: $data")
                }.onFailure { error ->
                    Log.d(TAG, "rescan: Fail: ${error.message}")
                    _uiState.update { it.copy(errorMessage = error.message.toString()) }
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