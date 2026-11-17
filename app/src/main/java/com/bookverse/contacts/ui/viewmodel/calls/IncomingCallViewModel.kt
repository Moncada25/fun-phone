package com.bookverse.contacts.ui.viewmodel.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.service.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomingCallViewModel(
    private val callId: String?,
    private val callRepository: CallRepository,
) : ViewModel() {
    private val _call = MutableStateFlow<UiCall?>(null)
    val call: StateFlow<UiCall?> = _call.asStateFlow()

    init {
        viewModelScope.launch {
            callRepository.calls.collectLatest { list ->
                _call.value = list.firstOrNull { it.id == callId } ?: list.firstOrNull()
            }
        }
    }

    companion object {
        fun provideFactory(
            callId: String?,
            callRepository: CallRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return IncomingCallViewModel(callId, callRepository) as T
            }
        }
    }
}
