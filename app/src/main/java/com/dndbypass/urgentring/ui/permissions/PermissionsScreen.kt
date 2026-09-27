@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dndbypass.urgentring.permissions.PermissionsHelper

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshTick++ }
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            key(refreshTick) {
                PermissionCard(
                    title = "Call screening",
                    description = "Lets Urgent Ring see incoming caller numbers before the phone rings.",
                    granted = PermissionsHelper.isCallScreeningRoleHeld(context),
                    onRequest = { settingsLauncher.launch(PermissionsHelper.requestCallScreeningRoleIntent(context)) }
                )
                PermissionCard(
                    title = "Battery optimization exemption",
                    description = "Keeps the call-screening service reliable on phones that aggressively kill background apps.",
                    granted = PermissionsHelper.isIgnoringBatteryOptimizations(context),
                    onRequest = { settingsLauncher.launch(PermissionsHelper.ignoreBatteryOptimizationsIntent(context)) }
                )
                PermissionCard(
                    title = "Full-screen alerts",
                    description = "Lets Urgent Ring show a full-screen incoming-call style alert over the lock screen.",
                    granted = PermissionsHelper.canUseFullScreenIntent(context),
                    onRequest = { settingsLauncher.launch(PermissionsHelper.fullScreenIntentSettingsIntent(context)) }
                )
                PermissionCard(
                    title = "Answer/end calls",
                    description = "Lets the incoming-call screen's Answer/Decline buttons act on the real call.",
                    granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) ==
                        PackageManager.PERMISSION_GRANTED,
                    onRequest = { runtimePermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS) }
                )
                PermissionCard(
                    title = "Detect when the call ends",
                    description = "Lets Urgent Ring stop the alert as soon as the call is answered or ends, " +
                        "instead of always waiting out a fixed timer.",
                    granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED,
                    onRequest = { runtimePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                )
                OemBackgroundPopupCard(
                    onOpenAppSettings = {
                        try {
                            settingsLauncher.launch(PermissionsHelper.miuiPermissionEditorIntent(context))
                        } catch (e: Exception) {
                            settingsLauncher.launch(PermissionsHelper.appInfoIntent(context))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OemBackgroundPopupCard(onOpenAppSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Background pop-up (some phones)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Xiaomi/MIUI and some other phone makers block apps from showing a full-screen " +
                    "alert from the background unless you separately allow it — Android has no " +
                    "setting we can check for this, so we can't show granted/not granted here. " +
                    "On the app's own settings page, look under Permissions (or \"Other " +
                    "permissions\") for something like \"Display pop-up windows while running " +
                    "in the background\" and turn it on, along with \"Show on Lock screen\" and " +
                    "\"Autostart\" if present.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onOpenAppSettings) { Text("Open app settings") }
        }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, granted: Boolean, onRequest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Row {
                Text(if (granted) "Granted" else "Not granted", style = MaterialTheme.typography.labelLarge)
            }
            if (!granted) {
                Button(onClick = onRequest) { Text("Grant") }
            }
        }
    }
}
