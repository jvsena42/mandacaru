package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import androidx.compose.runtime.Stable
import com.florestad.Network
import com.github.jvsena42.mandacaru.domain.model.UpdateStatus
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday


@Stable
data class SettingsUiState(
    val descriptorText: String = "",
    val electrumAddress: String = "",
    val nodeAddress: String = "",
    val nodeAddressError: Int? = null,
    val isNodeAddressValid: Boolean = false,
    val snackBarMessage: String = "",
    val selectedNetwork: String = "",
    val isLoading: Boolean = false,
    val descriptors: List<String> = emptyList(),
    val network: List<Network> = Network.entries,
    val isDescriptorsExpanded: Boolean = false,
    val isNetworkExpanded: Boolean = false,
    val isNodeExpanded: Boolean = false,
    val isAboutExpanded: Boolean = false,
    val isDonateExpanded: Boolean = false,
    val walletBirthdayYear: Int = WalletBirthday.defaultYear(),
    val isBirthdayExpanded: Boolean = false,
    val isBirthdayPickerOpen: Boolean = false,
    val pendingBirthdayYear: Int? = null,
    val updateStatus: UpdateStatus = UpdateStatus(),
    val isDownloading: Boolean = false,
    val isRescanning: Boolean = false,
    val rescanBlocksProcessed: Int? = null,
    val rescanBlocksTotal: Int? = null,
)
