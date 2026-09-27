@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.donate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/ammarsaa"
private const val KOFI_URL = "https://ko-fi.com/ammarsaa"

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support the project") },
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
            Text(
                "Urgent Ring is free and ad-free. If it's helped you, a donation goes a long way.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { openUrl(context, BUY_ME_A_COFFEE_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buy Me a Coffee")
            }
            OutlinedButton(
                onClick = { openUrl(context, KOFI_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ko-fi")
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found to open $url", Toast.LENGTH_LONG).show()
    }
}
