@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(
    onOpenRules: () -> Unit,
    onOpenActivityLog: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebugLog: () -> Unit,
    onOpenHelpFeedback: () -> Unit,
    onOpenDonate: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val featureEnabled by viewModel.featureEnabled.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Urgent Ring") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Urgent Ring", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (featureEnabled) "Repeat callers can ring through" else "Feature is off",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = featureEnabled, onCheckedChange = viewModel::setFeatureEnabled)
                }
            }

            Button(onClick = onOpenRules, modifier = Modifier.fillMaxWidth()) {
                Text("Manage caller rules")
            }
            Button(onClick = onOpenActivityLog, modifier = Modifier.fillMaxWidth()) {
                Text("View call activity")
            }
            Button(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Permissions setup")
            }
            Button(onClick = onOpenDebugLog, modifier = Modifier.fillMaxWidth()) {
                Text("Debug logs")
            }
            Button(onClick = onOpenHelpFeedback, modifier = Modifier.fillMaxWidth()) {
                Text("Help & feedback")
            }
            Button(onClick = onOpenDonate, modifier = Modifier.fillMaxWidth()) {
                Text("Donate now")
            }
        }
    }
}
