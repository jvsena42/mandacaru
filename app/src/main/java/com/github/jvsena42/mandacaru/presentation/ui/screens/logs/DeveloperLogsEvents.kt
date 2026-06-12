package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import android.net.Uri

sealed interface DeveloperLogsEvents {
    data class ShareLogs(val uri: Uri) : DeveloperLogsEvents
}
