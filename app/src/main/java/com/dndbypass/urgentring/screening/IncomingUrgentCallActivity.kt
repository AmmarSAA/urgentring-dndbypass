package com.dndbypass.urgentring.screening

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dndbypass.urgentring.data.DebugLogger
import com.dndbypass.urgentring.ui.theme.UrgentRingTheme

/**
 * Full-screen, incoming-call-style alert launched via a full-screen notification
 * intent when a call crosses the ring-through threshold. Answer/Decline act on the
 * real underlying cellular call via TelecomManager, not on anything of our own — we
 * never placed or own this call, we're just offering a faster way to act on it.
 */
class IncomingUrgentCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
            .requestDismissKeyguard(this, null)

        val callerName = intent.getStringExtra(EXTRA_NAME)
        val callerNumber = intent.getStringExtra(EXTRA_NUMBER)

        setContent {
            UrgentRingTheme {
                IncomingCallScreen(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    onAnswer = { answerCall(); finish() },
                    onDecline = { declineCall(); finish() }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun answerCall() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        runCatching { telecomManager.acceptRingingCall() }
            .onFailure { DebugLogger.log(this, "acceptRingingCall FAILED: ${it.message}") }
    }

    private fun declineCall() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        @Suppress("DEPRECATION")
        runCatching { telecomManager.endCall() }
            .onFailure { DebugLogger.log(this, "endCall FAILED: ${it.message}") }
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_NUMBER = "extra_number"
    }
}

@Composable
private fun IncomingCallScreen(
    callerName: String?,
    callerNumber: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1B1B1B)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Urgent Ring", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Repeat caller — ringing through",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(
                    modifier = Modifier.padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        callerName ?: callerNumber ?: "Unknown caller",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (callerName != null && callerNumber != null) {
                        Text(callerNumber, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(Icons.Filled.CallEnd, "Decline", Color(0xFFD32F2F), onDecline)
                CallActionButton(Icons.Filled.Call, "Answer", Color(0xFF2E7D32), onAnswer)
            }
        }
    }
}

@Composable
private fun CallActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(label, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}
