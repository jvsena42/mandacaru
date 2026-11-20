package com.github.jvsena42.mandacaru.presentation.ui.screens.settings.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.common.CoroutineDispatchers
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.tor.TorProxyProbe
import com.github.jvsena42.mandacaru.domain.settings.DEFAULT_TOR_PROXY
import com.github.jvsena42.mandacaru.domain.settings.TorProxyInput
import com.github.jvsena42.mandacaru.domain.settings.getCustomTorProxy
import com.github.jvsena42.mandacaru.domain.settings.isTorEnabled
import com.github.jvsena42.mandacaru.domain.settings.parseTorProxy
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class TorSettingsUiState(
    val isExpanded: Boolean = false,
    val isLoaded: Boolean = false,
    val isTorEnabled: Boolean = false,
    val savedTorEnabled: Boolean = false,
    val proxyText: String = "",
    val savedProxyText: String = "",
    val defaultProxy: String = DEFAULT_TOR_PROXY.toString(),
    val proxyError: Int? = null,
    val isApplying: Boolean = false,
) {
    val hasChanges: Boolean
        get() = isTorEnabled != savedTorEnabled ||
            (isTorEnabled && proxyText.trim() != savedProxyText)
}

sealed interface TorSettingsAction {
    data object ToggleExpanded : TorSettingsAction
    data class OnToggleTor(val enabled: Boolean) : TorSettingsAction
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
            val enabled = preferencesDataSource.isTorEnabled()
            val customProxy = preferencesDataSource.getCustomTorProxy()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    isTorEnabled = enabled,
                    savedTorEnabled = enabled,
                    proxyText = customProxy,
                    savedProxyText = customProxy,
                )
            }
        }
    }

    fun onAction(action: TorSettingsAction) {
        when (action) {
            TorSettingsAction.ToggleExpanded -> _uiState.update { it.copy(isExpanded = !it.isExpanded) }
            is TorSettingsAction.OnToggleTor -> _uiState.update {
                it.copy(isTorEnabled = action.enabled, proxyError = null)
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
        val proxyToSave = when (val parsed = parseTorProxy(state.proxyText)) {
            TorProxyInput.Default -> ""
            is TorProxyInput.Custom -> parsed.address.toString()
            TorProxyInput.Invalid -> {
                if (state.isTorEnabled) {
                    _uiState.update { it.copy(proxyError = R.string.tor_proxy_error_invalid) }
                    return
                }
                state.savedProxyText
            }
        }
        if (state.isTorEnabled == state.savedTorEnabled && proxyToSave == state.savedProxyText) {
            _uiState.update { it.copy(proxyText = proxyToSave, proxyError = null) }
            return
        }
        _uiState.update { it.copy(isApplying = true, proxyError = null) }
        viewModelScope.launch(dispatchers.io) {
            if (state.isTorEnabled && !isReachable(proxyToSave)) {
                _uiState.update {
                    it.copy(isApplying = false, proxyError = R.string.tor_proxy_error_unreachable)
                }
                return@launch
            }
            preferencesDataSource.setString(PreferenceKeys.CUSTOM_TOR_PROXY, proxyToSave)
            preferencesDataSource.setBoolean(PreferenceKeys.TOR_ENABLED, state.isTorEnabled)
            _uiState.update {
                it.copy(
                    savedTorEnabled = state.isTorEnabled,
                    proxyText = proxyToSave,
                    savedProxyText = proxyToSave,
                )
            }
            delay(RESTART_DELAY)
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
        val RESTART_DELAY = 2.seconds
        const val MAX_PROXY_LENGTH = 64
    }
}
