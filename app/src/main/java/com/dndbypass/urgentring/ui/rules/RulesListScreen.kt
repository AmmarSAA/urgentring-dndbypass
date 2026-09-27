@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dndbypass.urgentring.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndbypass.urgentring.data.CallerRule
import com.dndbypass.urgentring.data.DEFAULT_RULE_KEY

@Composable
fun RulesListScreen(
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RulesViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caller rules") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule) { Icon(Icons.Filled.Add, contentDescription = "Add rule") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules, key = { it.numberOrDefault }) { rule ->
                RuleRow(rule = rule, onClick = { onEditRule(rule.numberOrDefault) })
            }
        }
    }
}

@Composable
private fun RuleRow(rule: CallerRule, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = rule.displayName
                ?: if (rule.numberOrDefault == DEFAULT_RULE_KEY) "All callers" else rule.numberOrDefault
            Text(title, style = MaterialTheme.typography.titleMedium)
            val summary = when {
                rule.neverOverride -> "Never rings through"
                !rule.enabled -> "Disabled"
                else -> "${rule.thresholdCalls} calls within ${rule.windowMinutes} min rings through"
            }
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
