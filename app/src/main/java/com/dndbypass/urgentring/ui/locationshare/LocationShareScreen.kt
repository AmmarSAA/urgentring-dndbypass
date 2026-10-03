@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.locationshare

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndbypass.urgentring.data.LocationShareContact
import com.dndbypass.urgentring.ui.rules.resolveContactPhoneNumber

@Composable
fun LocationShareScreen(onBack: () -> Unit, viewModel: LocationShareViewModel = viewModel()) {
    val context = LocalContext.current
    val enabled by viewModel.enabled.collectAsState()
    val smsEnabled by viewModel.smsEnabled.collectAsState()
    val whatsAppEnabled by viewModel.whatsAppEnabled.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    var refreshTick by remember { mutableIntStateOf(0) }
    var manualNumber by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf(false) }

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshTick++ }
    val multiPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshTick++ }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val (name, number) = resolveContactPhoneNumber(context, uri)
        if (number != null) viewModel.addContact(name, number)
    }
    // PickContact() needs no permission, but resolving the number queries the Contacts provider,
    // which does — same reason EditRuleScreen asks first.
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) contactPicker.launch(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location sharing") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share my location on missed calls", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (enabled) "On — selected callers get your location" else "Off",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = viewModel::setEnabled)
                    }
                    Text(
                        "When you don't answer a call from someone listed below, Urgent Ring sends them " +
                            "your current location as a map link. Texts go out automatically and may use " +
                            "your carrier's SMS charges. WhatsApp can't be automated, so you get a " +
                            "notification to tap instead. A caller can only be messaged once per 15 " +
                            "minutes. Declining a call counts as not answering it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Caller ID can be faked, so only add people you trust.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            SectionTitle("How to send")
            ToggleRow("Text message (SMS), automatic", smsEnabled, viewModel::setSmsEnabled)
            ToggleRow("WhatsApp, tap a notification to send", whatsAppEnabled, viewModel::setWhatsAppEnabled)

            SectionTitle("Permissions")
            key(refreshTick) {
                val fineGranted = granted(Manifest.permission.ACCESS_FINE_LOCATION)
                PermissionRow(
                    title = "Location",
                    description = "Needed to find where you are.",
                    granted = fineGranted,
                    onGrant = {
                        multiPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                )
                PermissionRow(
                    title = "Location, all the time",
                    description = "Missed calls happen when the app is closed, so Android requires " +
                        "\"Allow all the time\". Grant Location first.",
                    granted = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    enabled = fineGranted,
                    onGrant = { permissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                )
                if (smsEnabled) {
                    PermissionRow(
                        title = "Send text messages",
                        description = "Needed to text your location automatically.",
                        granted = granted(Manifest.permission.SEND_SMS),
                        onGrant = { permissionLauncher.launch(Manifest.permission.SEND_SMS) }
                    )
                }
                PermissionRow(
                    title = "Detect missed calls",
                    description = "Lets Urgent Ring tell a missed call from an answered one.",
                    granted = granted(Manifest.permission.READ_PHONE_STATE),
                    onGrant = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                )
            }

            SectionTitle("Selected callers")
            if (contacts.isEmpty()) {
                Text(
                    "No one yet. Nobody receives your location until you add them here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            contacts.forEach { contact ->
                ContactRow(contact = contact, onRemove = { viewModel.remove(contact) })
            }

            OutlinedButton(
                onClick = {
                    if (granted(Manifest.permission.READ_CONTACTS)) {
                        contactPicker.launch(null)
                    } else {
                        contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add from contacts") }

            OutlinedTextField(
                value = manualNumber,
                onValueChange = {
                    manualNumber = it
                    numberError = false
                },
                label = { Text("Or type a phone number") },
                isError = numberError,
                supportingText = if (numberError) ({ Text("Enter a valid phone number") }) else null,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (viewModel.addContact(null, manualNumber)) manualNumber = "" else numberError = true
                },
                enabled = manualNumber.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add number") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
    enabled: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Text(if (granted) "Granted" else "Not granted", style = MaterialTheme.typography.labelLarge)
            }
            if (!granted) {
                Button(onClick = onGrant, enabled = enabled, modifier = Modifier.padding(start = 12.dp)) { Text("Grant") }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: LocationShareContact, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.displayName ?: contact.normalizedNumber, style = MaterialTheme.typography.titleMedium)
                if (contact.displayName != null) {
                    Text(contact.normalizedNumber, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    if (contact.lastSentMillis > 0) {
                        "Last sent " + DateUtils.getRelativeTimeSpanString(
                            contact.lastSentMillis,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                    } else {
                        "Never sent"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
        }
    }
}
