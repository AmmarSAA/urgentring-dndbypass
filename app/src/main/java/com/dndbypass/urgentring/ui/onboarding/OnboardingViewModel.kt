package com.dndbypass.urgentring.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.SettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepository = SettingsRepository(app)

    fun markCompleted() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
