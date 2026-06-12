package com.github.jvsena42.mandacaru.presentation.ui.screens.logs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenDeveloperLogs(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeveloperLogsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val shareLogsTitle = stringResource(R.string.share_logs)

    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DeveloperLogsEvents.ShareLogs -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, shareLogsTitle))
                }
            }
        }
    }

    ScreenDeveloperLogs(
        uiState = uiState,
        onBack = onBack,
        onCopy = {
            clipboardManager.setText(AnnotatedString(uiState.lines.joinToString("\n") { it.text }))
            viewModel.copiedToClipboard()
        },
        onShare = viewModel::share,
        onSnackBarShown = viewModel::clearSnackBarMessage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenDeveloperLogs(
    uiState: DeveloperLogsUiState,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSnackBarShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val currentOnSnackBarShown by rememberUpdatedState(onSnackBarShown)
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.snackBarMessage) {
        if (uiState.snackBarMessage.isNotEmpty()) {
            snackBarHostState.showSnackbar(uiState.snackBarMessage)
            currentOnSnackBarShown()
        }
    }

    // Keep the newest line in view as logs stream in, but only while the user is
    // already at the bottom — `layoutInfo` here still reflects the pre-update
    // layout, so it tells us whether they were following the tail.
    LaunchedEffect(uiState.lines) {
        val lines = uiState.lines
        if (lines.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        val wasAtBottom = lastVisible == -1 || lastVisible >= info.totalItemsCount - 1
        if (wasAtBottom) listState.scrollToItem(lines.lastIndex)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("button_back_logs"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.logs_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCopy,
                        enabled = uiState.lines.isNotEmpty(),
                        modifier = Modifier.testTag("button_copy_logs"),
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_logs),
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        enabled = uiState.lines.isNotEmpty(),
                        modifier = Modifier.testTag("button_export_logs"),
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share_logs),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.lines.isEmpty() -> Text(
                    text = stringResource(R.string.no_logs_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LogList(lines = uiState.lines, listState = listState)
            }
        }
    }
}

@Composable
private fun LogList(lines: List<LogLine>, listState: LazyListState) {
    val horizontalScroll = rememberScrollState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(lines) { line ->
            Text(
                text = line.text,
                color = line.level.color(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll),
            )
        }
    }
}

@Composable
private fun LogLevel.color(): Color = when (this) {
    LogLevel.ERROR -> Color(0xFFFF5370)
    LogLevel.WARN -> Color(0xFFFFCB6B)
    LogLevel.INFO -> Color(0xFFC3E88D)
    LogLevel.DEBUG -> Color(0xFF82AAFF)
    LogLevel.TRACE -> Color(0xFFC792EA)
    LogLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
}
