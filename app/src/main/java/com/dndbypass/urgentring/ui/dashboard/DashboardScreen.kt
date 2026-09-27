@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val TAPS_TO_REVEAL_DEBUG_LOG = 7

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
    var versionTapCount by remember { mutableIntStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("Urgent Ring") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Column {
                            Text("Urgent Ring", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (featureEnabled) "Repeat callers can ring through" else "Feature is off",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Switch(checked = featureEnabled, onCheckedChange = viewModel::setFeatureEnabled)
                }
            }

            DashboardSection(title = "Manage") {
                DashboardRow(Icons.Filled.PersonSearch, "Manage caller rules", onOpenRules)
                DashboardRow(Icons.Filled.History, "View call activity", onOpenActivityLog)
                DashboardRow(Icons.Filled.Shield, "Permissions setup", onOpenPermissions)
            }

            DashboardSection(title = "Support") {
                DashboardRow(Icons.AutoMirrored.Filled.HelpOutline, "Help & feedback", onOpenHelpFeedback)
                DashboardRow(Icons.Filled.Favorite, "Donate now", onOpenDonate)
            }

            Text(
                "Urgent Ring — version 0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable {
                        versionTapCount++
                        if (versionTapCount >= TAPS_TO_REVEAL_DEBUG_LOG) {
                            versionTapCount = 0
                            onOpenDebugLog()
                        }
                    }
            )
        }
    }
}

@Composable
private fun DashboardSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun DashboardRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
