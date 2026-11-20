package com.bookverse.contacts.ui.viewmodel.calls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.repository.ProfilePhoneNumbers
import com.bookverse.contacts.domain.call.CallScreeningOption
import com.bookverse.contacts.domain.call.CallScreeningSettings
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.domain.call.SimSelectionPolicy
import com.bookverse.contacts.service.CallScreeningRole
import com.bookverse.contacts.service.PhoneAccountDirectory
import com.bookverse.contacts.settings.CallPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CallSettingsUiState(
    val simAccounts: List<SimAccount> = emptyList(),
    val simPreference: SimPreference = SimPreference.AskEveryTime,
    val screeningSettings: CallScreeningSettings = CallScreeningSettings(),
    val screeningRoleActive: Boolean = false,
    val screeningRoleAvailable: Boolean = false,
    val hasProfileNumber: Boolean = false,
) {
    val hasMultipleSims: Boolean
        get() = simAccounts.size > 1

    val effectiveSimPreference: SimPreference
        get() = SimSelectionPolicy.effectivePreference(simAccounts, simPreference)
}

class CallSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CallSettingsUiState())
    val uiState: StateFlow<CallSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            CallPreferences.simPreference(application).collect { preference ->
                _uiState.update { it.copy(simPreference = preference) }
            }
        }
        viewModelScope.launch {
            CallPreferences.screeningSettings(application).collect { settings ->
                _uiState.update { it.copy(screeningSettings = settings) }
            }
        }
        refreshDeviceState()
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val accounts = withContext(Dispatchers.IO) {
                PhoneAccountDirectory.callCapableAccounts(context)
            }
            val hasProfileNumber = withContext(Dispatchers.IO) {
                ProfilePhoneNumbers.read(context).isNotEmpty()
            }
            val roleActive = CallScreeningRole.isActive(context)
            val roleAvailable = CallScreeningRole.isRoleAvailable(context)
            _uiState.update {
                it.copy(
                    simAccounts = accounts,
                    screeningRoleActive = roleActive,
                    screeningRoleAvailable = roleAvailable,
                    hasProfileNumber = hasProfileNumber,
                )
            }
        }
    }

    fun setSimPreference(preference: SimPreference) {
        viewModelScope.launch { CallPreferences.setSimPreference(getApplication(), preference) }
    }

    fun setScreeningOption(option: CallScreeningOption, enabled: Boolean) {
        viewModelScope.launch { CallPreferences.setScreeningOption(getApplication(), option, enabled) }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CallSettingsViewModel(application) as T
            }
    }
}
