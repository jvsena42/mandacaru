package com.github.jvsena42.mandacaru.presentation.service

import com.github.jvsena42.mandacaru.domain.floresta.SyncPhase
import com.github.jvsena42.mandacaru.domain.floresta.SyncSnapshot
import com.github.jvsena42.mandacaru.presentation.utils.toSyncPercentageString
import java.text.NumberFormat

/**
 * What the foreground-service notification should say for a given
 * [SyncPhase]. Kept free of Android types so the mapping — in particular
 * "never claim synced outside [SyncPhase.SYNCED]" — is unit-testable.
 */
data class SyncNotificationContent(
    val contentText: String,
    val subText: String,
    val progressPercent: Int?,
    val indeterminate: Boolean,
    val synced: Boolean,
)

private const val PERCENTAGE_MULTIPLIER = 100

fun syncNotificationContent(
    phase: SyncPhase,
    snapshot: SyncSnapshot,
    headerSyncDecimal: Float?,
    rescanProgressDecimal: Float?,
    height: Int,
): SyncNotificationContent = when (phase) {
    SyncPhase.HEADERS -> headerSyncDecimal
        ?.let { determinate(it, "Syncing headers", "headers") }
        ?: indeterminate("Syncing headers…", "Connecting to peers")

    SyncPhase.STALLED -> SyncNotificationContent(
        contentText = "Sync stalled at block #${formatHeight(height)}",
        subText = "Storage may be unhealthy",
        progressPercent = null,
        indeterminate = false,
        synced = false,
    )

    SyncPhase.BLOCKS -> determinate(snapshot.progress, "Syncing blocks", "blocks")

    SyncPhase.FILTERS -> determinate(
        decimal = snapshot.filterSyncDecimal ?: 0f,
        label = "Syncing filters",
        subLabel = "filters",
    )

    SyncPhase.WALLET_SCAN -> rescanProgressDecimal
        ?.let { determinate(it, "Scanning wallet", "wallet scan") }
        ?: indeterminate("Scanning wallet…", "Finding your transactions")

    SyncPhase.SYNCED -> SyncNotificationContent(
        contentText = "Synced - Block #${formatHeight(height)}",
        subText = "Fully synced",
        progressPercent = null,
        indeterminate = false,
        synced = true,
    )
}

private fun determinate(decimal: Float, label: String, subLabel: String): SyncNotificationContent {
    val percentage = decimal.toSyncPercentageString()
    return SyncNotificationContent(
        contentText = "$label: $percentage%",
        subText = "$percentage% $subLabel",
        progressPercent = (decimal * PERCENTAGE_MULTIPLIER).toInt(),
        indeterminate = false,
        synced = false,
    )
}

private fun indeterminate(contentText: String, subText: String) = SyncNotificationContent(
    contentText = contentText,
    subText = subText,
    progressPercent = null,
    indeterminate = true,
    synced = false,
)

private fun formatHeight(height: Int): String = NumberFormat.getNumberInstance().format(height)
