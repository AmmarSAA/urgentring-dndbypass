package com.dndbypass.urgentring.ui.rules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.CallerRule
import com.dndbypass.urgentring.data.DEFAULT_RULE_KEY
import com.dndbypass.urgentring.data.PhoneNumberNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditRuleUiState(
    val isDefaultRule: Boolean = false,
    val phoneNumber: String = "",
    val displayName: String = "",
    val thresholdCalls: Int = 3,
    val windowMinutes: Int = 10,
    val enabled: Boolean = true,
    val neverOverride: Boolean = false,
    val isNewRule: Boolean = true
)

class EditRuleViewModel(app: Application) : AndroidViewModel(app) {
    private val ruleDao = AppDatabase.get(app).callerRuleDao()

    private val _uiState = MutableStateFlow(EditRuleUiState())
    val uiState = _uiState.asStateFlow()

    fun load(ruleKey: String) {
        if (ruleKey == "new") {
            _uiState.value = EditRuleUiState(isNewRule = true)
            return
        }
        viewModelScope.launch {
            val rule = ruleDao.findByKey(ruleKey) ?: return@launch
            _uiState.value = EditRuleUiState(
                isDefaultRule = rule.numberOrDefault == DEFAULT_RULE_KEY,
                phoneNumber = if (rule.numberOrDefault == DEFAULT_RULE_KEY) "" else rule.numberOrDefault,
                displayName = rule.displayName.orEmpty(),
                thresholdCalls = rule.thresholdCalls,
                windowMinutes = rule.windowMinutes,
                enabled = rule.enabled,
                neverOverride = rule.neverOverride,
                isNewRule = false
            )
        }
    }

    fun update(transform: (EditRuleUiState) -> EditRuleUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val key = if (state.isDefaultRule) {
            DEFAULT_RULE_KEY
        } else {
            PhoneNumberNormalizer.normalize(state.phoneNumber) ?: return
        }
        viewModelScope.launch {
            ruleDao.upsert(
                CallerRule(
                    numberOrDefault = key,
                    displayName = state.displayName.ifBlank { null },
                    thresholdCalls = state.thresholdCalls.coerceAtLeast(1),
                    windowMinutes = state.windowMinutes.coerceAtLeast(1),
                    enabled = state.enabled,
                    neverOverride = state.neverOverride
                )
            )
            onSaved()
        }
    }
}
