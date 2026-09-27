package com.dndbypass.urgentring.ui.debuglog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebugLogViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).debugLogDao()

    val entries = dao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }
}
