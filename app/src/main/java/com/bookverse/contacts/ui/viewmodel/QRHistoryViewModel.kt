package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.bookverse.contacts.data.qr.QRHistoryEntity
import com.bookverse.contacts.data.qr.QRHistoryStore
import com.bookverse.contacts.data.qr.QRType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class QRHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<QRHistoryEntity> = emptyList(),
    val loadFailed: Boolean = false,
)

sealed interface QRHistoryEffect {
    data object MutationFailed : QRHistoryEffect
}

class QRHistoryViewModel(
    private val store: QRHistoryStore,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    val uiState: StateFlow<QRHistoryUiState> = store.history
        .map { items -> QRHistoryUiState(isLoading = false, items = items) }
        .catch { emit(QRHistoryUiState(isLoading = false, loadFailed = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QRHistoryUiState(),
        )

    private val _effects = MutableSharedFlow<QRHistoryEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<QRHistoryEffect> = _effects.asSharedFlow()

    fun addQRHistory(content: String, isUrl: Boolean, type: QRType) {
        viewModelScope.launch(workerDispatcher) {
            runCatching {
                store.insert(
                    QRHistoryEntity(
                        content = content,
                        isUrl = isUrl,
                        type = type,
                    ),
                )
            }.exceptionOrNull()?.let {
                _effects.emit(QRHistoryEffect.MutationFailed)
            }
        }
    }

    fun deleteQRHistory(id: Long) {
        viewModelScope.launch(workerDispatcher) {
            runCatching { store.delete(id) }.exceptionOrNull()?.let {
                _effects.emit(QRHistoryEffect.MutationFailed)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(workerDispatcher) {
            runCatching { store.clear() }.exceptionOrNull()?.let {
                _effects.emit(QRHistoryEffect.MutationFailed)
            }
        }
    }
}
