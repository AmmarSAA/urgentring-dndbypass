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
    private val locationShareEnabledKey = booleanPreferencesKey("location_share_enabled")
    private val locationShareSmsKey = booleanPreferencesKey("location_share_sms")
    private val locationShareWhatsAppKey = booleanPreferencesKey("location_share_whatsapp")

    val featureEnabled: Flow<Boolean> = context.dataStore.data.map { it[featureEnabledKey] ?: false }

    suspend fun setFeatureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[featureEnabledKey] = enabled }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompletedKey] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[onboardingCompletedKey] = completed }
    }

    // Missed-call location sharing is opt-in and off until the user turns it on in its own
    // settings screen; it is never switched on by onboarding or by the main feature toggle.
    val locationShareEnabled: Flow<Boolean> = context.dataStore.data.map { it[locationShareEnabledKey] ?: false }

    suspend fun setLocationShareEnabled(enabled: Boolean) {
        context.dataStore.edit { it[locationShareEnabledKey] = enabled }
    }

    val locationShareSms: Flow<Boolean> = context.dataStore.data.map { it[locationShareSmsKey] ?: true }

    suspend fun setLocationShareSms(enabled: Boolean) {
        context.dataStore.edit { it[locationShareSmsKey] = enabled }
    }

    val locationShareWhatsApp: Flow<Boolean> = context.dataStore.data.map { it[locationShareWhatsAppKey] ?: false }

    suspend fun setLocationShareWhatsApp(enabled: Boolean) {
        context.dataStore.edit { it[locationShareWhatsAppKey] = enabled }
    }
}
