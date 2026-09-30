package com.dndbypass.urgentring.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "urgent_ring_settings")

class SettingsRepository(private val context: Context) {
    private val featureEnabledKey = booleanPreferencesKey("feature_enabled")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    val featureEnabled: Flow<Boolean> = context.dataStore.data.map { it[featureEnabledKey] ?: false }

    suspend fun setFeatureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[featureEnabledKey] = enabled }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompletedKey] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[onboardingCompletedKey] = completed }
    }
}
