package com.dndbypass.urgentring.ui.activitylog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ActivityLogViewModel(app: Application) : AndroidViewModel(app) {
    private val eventDao = AppDatabase.get(app).callEventDao()

    val events = eventDao.recentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
