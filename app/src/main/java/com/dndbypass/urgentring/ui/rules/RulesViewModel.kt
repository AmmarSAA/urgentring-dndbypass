package com.dndbypass.urgentring.ui.rules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.CallerRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(app: Application) : AndroidViewModel(app) {
    private val ruleDao = AppDatabase.get(app).callerRuleDao()

    // The default "all callers" rule is seeded at app startup (UrgentRingApplication),
    // not here, so it exists even if the user never opens this screen.
    val rules = ruleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(rule: CallerRule) {
        viewModelScope.launch { ruleDao.delete(rule) }
    }
}
