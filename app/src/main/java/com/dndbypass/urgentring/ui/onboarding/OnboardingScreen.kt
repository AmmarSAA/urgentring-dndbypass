package com.dndbypass.urgentring.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndbypass.urgentring.permissions.PermissionsHelper

private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val launch: (
        settingsLauncher: ActivityResultLauncher<Intent>,
        permissionLauncher: ActivityResultLauncher<String>
    ) -> Unit
)

private fun buildOnboardingSteps(context: Context): List<OnboardingStep> = buildList {
    add(
        OnboardingStep(
            title = "Call screening",
            description = "Lets Urgent Ring see incoming caller numbers before the phone rings, " +
                "so it can detect repeat callers. This is the core permission the app needs to work.",
            icon = Icons.Filled.Shield,
            launch = { settingsLauncher, _ ->
                settingsLauncher.launch(PermissionsHelper.requestCallScreeningRoleIntent(context))
            }
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            OnboardingStep(
                title = "Notifications",
                description = "Needed to show the full-screen incoming-call alert when a repeat caller rings through.",
                icon = Icons.Filled.NotificationsActive,
                launch = { _, permissionLauncher -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        add(
            OnboardingStep(
                title = "Full-screen alerts",
                description = "Lets the incoming-call screen show over your lock screen, like a real call.",
                icon = Icons.Filled.Fullscreen,
                launch = { settingsLauncher, _ ->
                    settingsLauncher.launch(PermissionsHelper.fullScreenIntentSettingsIntent(context))
                }
            )
        )
    }
    add(
        OnboardingStep(
            title = "Contacts",
            description = "Lets Urgent Ring show a caller's saved name on the alert screen instead of just their number.",
            icon = Icons.Filled.PersonSearch,
            launch = { _, permissionLauncher -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
        )
    )
    add(
        OnboardingStep(
            title = "Answer/end calls",
            description = "Lets the incoming-call screen's Answer/Decline buttons act on the real call.",
            icon = Icons.Filled.Call,
            launch = { _, permissionLauncher -> permissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS) }
        )
    )
    add(
        OnboardingStep(
            title = "Detect when the call ends",
            description = "Lets Urgent Ring stop the alert as soon as you answer or the call ends, " +
                "instead of always waiting out a fixed timer.",
            icon = Icons.Filled.History,
            launch = { _, permissionLauncher -> permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
        )
    )
    add(
        OnboardingStep(
            title = "Battery optimization exemption",
            description = "Keeps the call-screening service reliable on phones that aggressively kill background apps.",
            icon = Icons.Filled.BatteryChargingFull,
            launch = { settingsLauncher, _ ->
                settingsLauncher.launch(PermissionsHelper.ignoreBatteryOptimizationsIntent(context))
            }
        )
    )
}

/**
 * Walks a first-time user through every permission the app can use, one at a time, right
 * after install — rather than leaving them to discover the Permissions screen on their own
 * and wonder why the feature doesn't work. Declining a step just skips it; nothing here is
 * force-required, since RingerOverrideManager and friends already degrade gracefully when a
 * permission is missing.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val context = LocalContext.current
    val steps = remember { buildOnboardingSteps(context) }
    var stepIndex by remember { mutableIntStateOf(0) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { stepIndex++ }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { stepIndex++ }

    LaunchedEffect(stepIndex) {
        if (stepIndex >= steps.size) {
            viewModel.markCompleted()
            onFinished()
        }
    }

    if (stepIndex >= steps.size) return

    val step = steps[stepIndex]
    val isLastStep = stepIndex == steps.lastIndex

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                LinearProgressIndicator(
                    progress = { (stepIndex + 1f) / steps.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Step ${stepIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(step.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { step.launch(settingsLauncher, permissionLauncher) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow")
                }
                TextButton(onClick = { stepIndex++ }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isLastStep) "Finish" else "Skip for now")
                }
            }
        }
    }
}
