package com.github.jvsena42.mandacaru.presentation.utils

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.TransactionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDate(): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        .format(Date(this * 1000))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - this

    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60} minutes ago"
        diff < 86400 -> "${diff / 3600} hours ago"
        diff < 2592000 -> "${diff / 86400} days ago"
        diff < 31536000 -> "${diff / 2592000} months ago"
        else -> "${diff / 31536000} years ago"
    }
}

fun TransactionResult.getTotalOutputValue(): Double {
    return vout?.sumOf { it.value ?: 0.0 } ?: 0.0
}

fun TransactionResult.getEstimatedFee(): String {
    val outputValue = getTotalOutputValue()
    // Fee calculation would require input values
    return "N/A"
}

fun Int.getConfirmationStatus(): ConfirmationStatus {
    return when {
        this == 0 -> ConfirmationStatus.UNCONFIRMED
        this < 6 -> ConfirmationStatus.PENDING
        else -> ConfirmationStatus.CONFIRMED
    }
}

enum class ConfirmationStatus {
    UNCONFIRMED,
    PENDING,
    CONFIRMED
}

fun String.truncateMiddle(maxLength: Int = 16): String {
    if (this.length <= maxLength) return this
    val charsToShow = maxLength - 3
    val frontChars = charsToShow / 2
    val backChars = charsToShow - frontChars
    return "${this.take(frontChars)}...${this.takeLast(backChars)}"
}