@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.rules

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EditRuleScreen(ruleKey: String, onDone: () -> Unit, viewModel: EditRuleViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(ruleKey) { viewModel.load(ruleKey) }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val (name, number) = resolveContactPhoneNumber(context, uri)
        viewModel.update { it.copy(displayName = name ?: it.displayName, phoneNumber = number ?: it.phoneNumber) }
    }

    // PickContact() itself needs no permission (it delegates to the Contacts app), but
    // resolveContactPhoneNumber() below queries the Contacts provider directly, which
    // requires READ_CONTACTS to be granted at runtime — request it first or that query
    // throws a SecurityException and crashes.
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(null)
    }

    fun pickContact() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) contactPicker.launch(null) else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isDefaultRule) "All callers" else "Caller rule") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
            if (!state.isDefaultRule) {
                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = { value -> viewModel.update { it.copy(phoneNumber = value) } },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isNewRule
                )
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { value -> viewModel.update { it.copy(displayName = value) } },
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isNewRule) {
                    OutlinedButton(onClick = { pickContact() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pick from contacts")
                    }
                }
            }

            Text("Ring through after ${state.thresholdCalls} call(s) within ${state.windowMinutes} minute(s)")

            Text("Call threshold")
            Slider(
                value = state.thresholdCalls.toFloat(),
                onValueChange = { value -> viewModel.update { it.copy(thresholdCalls = value.toInt()) } },
                valueRange = 1f..10f,
                steps = 8
            )

            Text("Time window (minutes)")
            Slider(
                value = state.windowMinutes.toFloat(),
                onValueChange = { value -> viewModel.update { it.copy(windowMinutes = value.toInt()) } },
                valueRange = 1f..60f,
                steps = 58
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = state.enabled, onCheckedChange = { value -> viewModel.update { it.copy(enabled = value) } })
            }

            if (!state.isDefaultRule) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Never ring through (exclude from this feature)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.neverOverride,
                        onCheckedChange = { value -> viewModel.update { it.copy(neverOverride = value) } }
                    )
                }
            }

            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}

internal fun resolveContactPhoneNumber(context: Context, contactUri: Uri): Pair<String?, String?> = try {
    resolveContactPhoneNumberUnsafe(context, contactUri)
} catch (e: SecurityException) {
    null to null
}

private fun resolveContactPhoneNumberUnsafe(context: Context, contactUri: Uri): Pair<String?, String?> {
    context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null to null
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
        val contactId = if (idIndex >= 0) cursor.getString(idIndex) else null
        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
        val hasPhone = hasPhoneIndex >= 0 && cursor.getInt(hasPhoneIndex) > 0

        if (contactId != null && hasPhone) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val number = if (numberIndex >= 0) phoneCursor.getString(numberIndex) else null
                    return name to number
                }
            }
        }
        return name to null
    }
    return null to null
}
