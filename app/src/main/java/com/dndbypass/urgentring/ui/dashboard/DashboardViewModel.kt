package com.dndbypass.urgentring.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepository = SettingsRepository(app)

    val featureEnabled = settingsRepository.featureEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setFeatureEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFeatureEnabled(enabled) }
    }
}
