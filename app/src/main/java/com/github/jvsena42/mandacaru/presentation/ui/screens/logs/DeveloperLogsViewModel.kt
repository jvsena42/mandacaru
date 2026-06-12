package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.utils.EventFlow
import com.github.jvsena42.mandacaru.presentation.utils.EventFlowImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class DeveloperLogsViewModel(
    @field:SuppressLint("StaticFieldLeak") private val context: Context,
) : ViewModel(), EventFlow<DeveloperLogsEvents> by EventFlowImpl() {

    private val _uiState = MutableStateFlow(DeveloperLogsUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        observeLogs()
    }

    private fun observeLogs() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val logFile = File(context.filesDir, LOG_FILE_NAME)
                val lines = if (logFile.exists()) {
                    LogParser.parse(LogTailReader.tail(logFile.readLines()).joinToString("\n"))
                } else {
                    emptyList()
                }
                _uiState.update { it.copy(lines = lines, isLoading = false) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun copiedToClipboard() {
        _uiState.update {
            it.copy(snackBarMessage = context.getString(R.string.logs_copied_to_clipboard))
        }
    }

    fun clearSnackBarMessage() {
        _uiState.update { it.copy(snackBarMessage = "") }
    }

    fun share() {
        viewModelScope.launch(Dispatchers.IO) {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (!logFile.exists()) {
                _uiState.update {
                    it.copy(snackBarMessage = context.getString(R.string.log_file_not_found))
                }
                return@launch
            }

            val cacheDir = File(context.cacheDir, "logs").apply { mkdirs() }
            val cachedLog = File(cacheDir, LOG_FILE_NAME)
            logFile.copyTo(cachedLog, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cachedLog
            )
            viewModelScope.sendEvent(DeveloperLogsEvents.ShareLogs(uri))
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    private companion object {
        const val LOG_FILE_NAME = "debug.log"
        const val POLL_INTERVAL_MS = 1500L
    }
}
