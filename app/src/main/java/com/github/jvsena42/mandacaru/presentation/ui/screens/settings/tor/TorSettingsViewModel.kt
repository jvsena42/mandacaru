package com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.common.CoroutineDispatchers
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.tor.TorProxyProbe
import com.github.jvsena42.mandacaru.domain.settings.DEFAULT_TOR_PROXY
import com.github.jvsena42.mandacaru.domain.settings.TorMode
import com.github.jvsena42.mandacaru.domain.settings.TorProxyInput
import com.github.jvsena42.mandacaru.domain.settings.getCustomTorProxy
import com.github.jvsena42.mandacaru.domain.settings.getTorMode
import com.github.jvsena42.mandacaru.domain.settings.parseTorProxy
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TorSettingsUiState(
    val isExpanded: Boolean = false,
    val isLoaded: Boolean = false,
    val mode: TorMode = TorMode.OFF,
    val savedMode: TorMode = TorMode.OFF,
    val proxyText: String = "",
    val savedProxyText: String = "",
    val defaultProxy: String = DEFAULT_TOR_PROXY.toString(),
    val proxyError: Int? = null,
    val isApplying: Boolean = false,
) {
    val usesTor: Boolean
        get() = mode != TorMode.OFF

    val hasChanges: Boolean
        get() = mode != savedMode || (usesTor && proxyText.trim() != savedProxyText)
}

sealed interface TorSettingsAction {
    data object ToggleExpanded : TorSettingsAction
    data class OnModeSelected(val mode: TorMode) : TorSettingsAction
    data class OnProxyChanged(val text: String) : TorSettingsAction
    data object OnClickApply : TorSettingsAction
}

sealed interface TorSettingsEvent {
    data object RestartRequired : TorSettingsEvent
}

class TorSettingsViewModel(
    private val preferencesDataSource: PreferencesDataSource,
    private val torProxyProbe: TorProxyProbe,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers(),
) : ViewModel(), EventFlow<TorSettingsEvent> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(TorSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val mode = preferencesDataSource.getTorMode()
            val customProxy = preferencesDataSource.getCustomTorProxy()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    mode = mode,
                    savedMode = mode,
                    proxyText = customProxy,
                    savedProxyText = customProxy,
                )
            }
        }
    }

    fun onAction(action: TorSettingsAction) {
        when (action) {
            TorSettingsAction.ToggleExpanded -> _uiState.update { it.copy(isExpanded = !it.isExpanded) }
            is TorSettingsAction.OnModeSelected -> _uiState.update {
                it.copy(mode = action.mode, proxyError = null)
            }
            is TorSettingsAction.OnProxyChanged -> _uiState.update {
                it.copy(
                    proxyText = action.text.take(MAX_PROXY_LENGTH),
                    proxyError = null,
                )
            }
            TorSettingsAction.OnClickApply -> apply()
        }
    }

    private fun apply() {
        val state = _uiState.value
        if (!state.isLoaded || state.isApplying || !state.hasChanges) return
        // Turning Tor off keeps the saved proxy: an edit made then is hidden and never probed.
        val proxyToSave = if (!state.usesTor) {
            state.savedProxyText
        } else {
            when (val parsed = parseTorProxy(state.proxyText)) {
                TorProxyInput.Default -> ""
                is TorProxyInput.Custom -> parsed.address.toString()
                TorProxyInput.Invalid -> {
                    _uiState.update { it.copy(proxyError = R.string.tor_proxy_error_invalid) }
                    return
                }
            }
        }
        if (state.mode == state.savedMode && proxyToSave == state.savedProxyText) {
            _uiState.update { it.copy(proxyText = proxyToSave, proxyError = null) }
            return
        }
        _uiState.update { it.copy(isApplying = true, proxyError = null) }
        viewModelScope.launch(dispatchers.io) {
            if (state.usesTor && !isReachable(proxyToSave)) {
                _uiState.update {
                    it.copy(isApplying = false, proxyError = R.string.tor_proxy_error_unreachable)
                }
                return@launch
            }
            preferencesDataSource.setString(PreferenceKeys.CUSTOM_TOR_PROXY, proxyToSave)
            preferencesDataSource.setString(PreferenceKeys.TOR_MODE, state.mode.name)
            _uiState.update {
                it.copy(
                    savedMode = state.mode,
                    proxyText = proxyToSave,
                    savedProxyText = proxyToSave,
                )
            }
            viewModelScope.sendEvent(TorSettingsEvent.RestartRequired)
        }
    }

    private suspend fun isReachable(proxyText: String): Boolean {
        val address = when (val parsed = parseTorProxy(proxyText)) {
            is TorProxyInput.Custom -> parsed.address
            else -> DEFAULT_TOR_PROXY
        }
        return torProxyProbe.isReachable(address)
    }

    private companion object {
        const val MAX_PROXY_LENGTH = 64
    }
}
