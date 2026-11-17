package com.bookverse.contacts.ui.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.security.CryptoManager
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface DraftCipher {
    suspend fun seal(fields: Map<String, String>): ByteArray
    suspend fun open(blob: ByteArray): Map<String, String>
}

private class AndroidDraftCipher(context: Context) : DraftCipher {
    private val appContext = context.applicationContext
    override suspend fun seal(fields: Map<String, String>): ByteArray = withContext(Dispatchers.IO) {
        checkNotNull(CryptoManager.encrypt(appContext, JSONObject(fields).toString().toByteArray(Charsets.UTF_8)))
    }
    override suspend fun open(blob: ByteArray): Map<String, String> = withContext(Dispatchers.IO) {
        val json = JSONObject(checkNotNull(CryptoManager.decrypt(appContext, blob)).toString(Charsets.UTF_8))
        json.keys().asSequence().associateWith { json.getString(it) }
    }
}

/** Only ciphertext enters SavedStateHandle; clear text belongs to an authenticated editor session. */
class EncryptedDraftViewModel(
    private val savedState: SavedStateHandle,
    private val cipher: DraftCipher,
    session: StateFlow<Boolean>,
) : ViewModel() {
    private val mutex = Mutex()
    private var saveJob: Job? = null
    private val _fields = MutableStateFlow<Map<String, String>?>(null)
    val fields = _fields.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    var initialized = false
        private set

    init {
        viewModelScope.launch {
            session.collect { unlocked ->
                if (!unlocked) {
                    saveJob?.cancel()
                    val pending = _fields.value?.takeIf { initialized }
                    _fields.value = null
                    if (pending != null) persist(pending)
                } else {
                    mutex.withLock {
                        val blob = savedState.get<ByteArray>("encrypted_draft")
                        try {
                            _fields.value = if (blob == null) emptyMap() else cipher.open(blob)
                            initialized = blob != null
                            _failed.value = false
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            _failed.value = true
                            _fields.value = emptyMap()
                            initialized = false
                        }
                    }
                }
            }
        }
    }

    fun initialize(values: Map<String, String>) {
        if (_fields.value == null || initialized) return
        initialized = true
        update(values)
    }

    fun update(values: Map<String, String>) {
        if (_fields.value == null) return
        _fields.value = values
        // Persist each edit so process recreation never falls back to a clear-text Bundle.
        saveJob?.cancel()
        saveJob = viewModelScope.launch { persist(values) }
    }

    fun field(key: String, snapshot: Map<String, String>?): MutableState<String> = object : MutableState<String> {
        override var value: String
            get() = snapshot?.get(key).orEmpty()
            set(value) { update((_fields.value ?: return) + (key to value)) }
        override fun component1(): String = value
        override fun component2(): (String) -> Unit = { value = it }
    }

    fun clear() {
        saveJob?.cancel()
        _fields.value = emptyMap()
        savedState.remove<ByteArray>("encrypted_draft")
        initialized = false
    }

    fun sealFinalSnapshot(values: Map<String, String>) {
        if (!initialized) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch { persist(values) }
    }

    private suspend fun persist(values: Map<String, String>) {
        mutex.withLock {
            try {
                savedState["encrypted_draft"] = cipher.seal(values)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _failed.value = true
            }
        }
    }
}

@Composable
fun encryptedDraft(key: String, settings: AppSettingsViewModel): EncryptedDraftViewModel {
    val context = LocalContext.current.applicationContext
    val factory = remember(context, settings) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return EncryptedDraftViewModel(extras.createSavedStateHandle(), AndroidDraftCipher(context), settings.protectedSessionUnlocked) as T
            }
        }
    }
    return viewModel(key = key, factory = factory)
}

@Composable
fun ReportDraftFailure(draft: EncryptedDraftViewModel) {
    val failed = draft.failed.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val message = androidx.compose.ui.res.stringResource(com.bookverse.contacts.R.string.protected_draft_error)
    androidx.compose.runtime.LaunchedEffect(failed) {
        if (failed) android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }
}

/** Restores once per authenticated composition, then seals snapshots of editable fields. */
@Composable
fun BindProtectedDraft(
    draft: EncryptedDraftViewModel?,
    snapshot: () -> Map<String, String>,
    restore: (Map<String, String>) -> Unit,
) {
    if (draft == null) return
    val fields = draft.fields.collectAsStateWithLifecycle().value
    var ready by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(draft, fields != null) {
        if (fields != null && !ready) {
            if (draft.initialized) restore(fields) else draft.initialize(snapshot())
            ready = true
        }
    }
    androidx.compose.runtime.LaunchedEffect(draft, ready) {
        if (ready) androidx.compose.runtime.snapshotFlow(snapshot).collect { draft.update(it) }
    }
    ReportDraftFailure(draft)
}
