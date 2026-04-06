package com.github.jvsena42.mandacaru.presentation.utils

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.TransactionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_DAY = 86400L
private const val SECONDS_PER_MONTH = 2592000L
private const val SECONDS_PER_YEAR = 31536000L
private const val CONFIRMATION_THRESHOLD = 6
private const val ELLIPSIS_LENGTH = 3

fun Long.toFormattedDate(): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        .format(Date(this * MILLIS_PER_SECOND))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis() / MILLIS_PER_SECOND
    val diff = now - this

    return when {
        diff < SECONDS_PER_MINUTE -> "Just now"
        diff < SECONDS_PER_HOUR -> "${diff / SECONDS_PER_MINUTE} minutes ago"
        diff < SECONDS_PER_DAY -> "${diff / SECONDS_PER_HOUR} hours ago"
        diff < SECONDS_PER_MONTH -> "${diff / SECONDS_PER_DAY} days ago"
        diff < SECONDS_PER_YEAR -> "${diff / SECONDS_PER_MONTH} months ago"
        else -> "${diff / SECONDS_PER_YEAR} years ago"
    }
}

fun TransactionResult.getTotalOutputValue(): Double {
    return vout?.sumOf { it.value ?: 0.0 } ?: 0.0
}

fun Int.getConfirmationStatus(): ConfirmationStatus {
    return when {
        this == 0 -> ConfirmationStatus.UNCONFIRMED
        this < CONFIRMATION_THRESHOLD -> ConfirmationStatus.PENDING
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
    val charsToShow = maxLength - ELLIPSIS_LENGTH
    val frontChars = charsToShow / 2
    val backChars = charsToShow - frontChars
    return "${this.take(frontChars)}...${this.takeLast(backChars)}"
}
