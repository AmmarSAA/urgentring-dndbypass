@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.debuglog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndbypass.urgentring.data.DebugLogEntry
import com.dndbypass.urgentring.data.DebugLogger
import kotlinx.coroutines.launch

@Composable
fun DebugLogScreen(onBack: () -> Unit, viewModel: DebugLogViewModel = viewModel()) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        copyLogsToClipboard(context, entries)
                        scope.launch { snackbarHostState.showSnackbar("Copied ${entries.size} log lines to clipboard") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Copy logs to clipboard")
                }
                OutlinedButton(onClick = { viewModel.clear() }) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    "No log entries yet. Enable the feature, make a test call, then come back here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Text(DebugLogger.format(entry), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun copyLogsToClipboard(context: Context, entries: List<DebugLogEntry>) {
    val text = entries.reversed().joinToString("\n") { DebugLogger.format(it) }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Urgent Ring debug logs", text))
}
