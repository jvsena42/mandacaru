package com.github.jvsena42.mandacaru.presentation.ui.screens.settings

import androidx.compose.runtime.Stable
import com.florestad.Network
import com.github.jvsena42.mandacaru.domain.geoip.PEER_FLAGS_ENABLED_BY_DEFAULT
import com.github.jvsena42.mandacaru.domain.model.UpdateStatus
import com.github.jvsena42.mandacaru.presentation.utils.DescriptorUtils
import com.github.jvsena42.mandacaru.presentation.utils.WalletBirthday

data class PendingDescriptor(
    val descriptor: String,
    val summary: DescriptorUtils.DescriptorSummary,
)

@Stable
data class SettingsUiState(
    val descriptorText: String = "",
    val isDescriptorScanSheetOpen: Boolean = false,
    val descriptorScanProgress: Float = 0f,
    val descriptorScanError: String = "",
    val pendingScannedDescriptor: PendingDescriptor? = null,
    val descriptorToShare: String? = null,
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
    val isRescanning: Boolean = false,
    val rescanBlocksProcessed: Int? = null,
    val rescanBlocksTotal: Int? = null,
    val useAlsoMobileData: Boolean = false,
    val isDataUsageExpanded: Boolean = false,
    val enableAdvancedFeatures: Boolean = false,
    val isPeerFlagsEnabled: Boolean = PEER_FLAGS_ENABLED_BY_DEFAULT,
    val isPeerFlagsExpanded: Boolean = false,
    val isDeveloperToolsExpanded: Boolean = false,
)
