package com.dndbypass.urgentring.ui.locationshare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.LocationShareContact
import com.dndbypass.urgentring.data.PhoneNumberNormalizer
import com.dndbypass.urgentring.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationShareViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val dao = AppDatabase.get(app).locationShareContactDao()

    val enabled = settings.locationShareEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val smsEnabled = settings.locationShareSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val whatsAppEnabled = settings.locationShareWhatsApp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val contacts = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { settings.setLocationShareEnabled(value) }
    }

    fun setSmsEnabled(value: Boolean) {
        viewModelScope.launch { settings.setLocationShareSms(value) }
    }

    fun setWhatsAppEnabled(value: Boolean) {
        viewModelScope.launch { settings.setLocationShareWhatsApp(value) }
    }

    /** Returns false if [rawNumber] isn't a usable phone number. */
    fun addContact(displayName: String?, rawNumber: String): Boolean {
        val normalized = PhoneNumberNormalizer.normalize(rawNumber) ?: return false
        viewModelScope.launch {
            dao.upsert(LocationShareContact(normalized, displayName?.takeIf { it.isNotBlank() }))
        }
        return true
    }

    fun remove(contact: LocationShareContact) {
        viewModelScope.launch { dao.delete(contact) }
    }
}
