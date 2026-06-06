package com.github.jvsena42.mandacaru.presentation.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.data.AppUpdateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    val isUpdateBadgeVisible = appUpdateRepository.updateStatus
        .map { it.isBadgeVisible }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch { appUpdateRepository.refresh(force = true) }
    }
}
