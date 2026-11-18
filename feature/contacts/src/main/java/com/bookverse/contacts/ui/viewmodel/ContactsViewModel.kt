package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.ui.screens.dialer.DialerDirectory
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CallLog
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentOperationSource
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.data.model.idsBySource
import com.bookverse.contacts.data.repository.BlockedNumberInfo
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.data.repository.ContactImportRepository
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.ContactVCardRepository
import com.bookverse.contacts.data.repository.InCallBackgroundChange
import com.bookverse.contacts.data.repository.IncomingCallBackgroundChange
import com.bookverse.contacts.data.repository.ProfileRepository
import com.bookverse.contacts.data.repository.RecentOperationsRepository
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.telemetry.ContactAction
import com.bookverse.contacts.domain.telemetry.FunnelTelemetry
import com.bookverse.contacts.domain.contacts.ContactInputPolicy
import com.bookverse.contacts.repository.ContactUsageRepository
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.applyEditableFields
import com.bookverse.contacts.utils.ImageProcessor
import com.bookverse.contacts.utils.PermissionUtils
import com.bookverse.contacts.utils.PreparedContactPhoto
import com.google.gson.GsonBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import contacts.core.Contacts
import contacts.core.entities.EmailEntity
import contacts.core.entities.EventEntity
import contacts.core.entities.ExistingContactEntity
import contacts.core.entities.PhoneEntity
import contacts.core.equalTo
import contacts.core.util.addEmail
import contacts.core.util.addEvent
import contacts.core.util.addPhone
import contacts.core.util.addWebsite
import contacts.core.util.removeAllWebsites
import contacts.core.util.setName
import contacts.core.util.setNote
import contacts.core.util.setOptions
import contacts.core.util.setOrganization
import contacts.core.util.shareVCardIntent
import contacts.permissions.profile.insertWithPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap
import java.util.LinkedHashSet
import kotlin.time.Duration.Companion.milliseconds

sealed interface ContactSaveResult {
    data class Success(val contact: Contact) : ContactSaveResult

    data class SuccessWithPhotoWarning(val contact: Contact) : ContactSaveResult

    data object Failure : ContactSaveResult
}

class ContactsViewModel(
    application: Application,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val contactCacheRepository: ContactCacheRepository,
    private val recentOperationsRepository: RecentOperationsRepository,
    private val contactImportRepository: ContactImportRepository,
    private val contactRecycleBin: ContactRecycleBin,
    contactVCardRepository: ContactVCardRepository,
    private val contactUsageRepository: ContactUsageRepository,
    private val funnelTelemetry: FunnelTelemetry,
    private val refreshWidgets: () -> Unit,
) : AndroidViewModel(application), DialerDirectory {

    private val application = getApplication<Application>()
    private val contactPhotoUpdateResolver = ContactPhotoUpdateResolver(
        contactRepository = contactRepository,
        profileRepository = profileRepository,
    )

    private fun localizedContext(): Context = LocaleController.localizedContext(application)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    override val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _profile = MutableStateFlow<Contact?>(null)
    override val profile: StateFlow<Contact?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    private val _selectedContacts = MutableStateFlow<Set<String>>(emptySet())
    val selectedContacts: StateFlow<Set<String>> = _selectedContacts.asStateFlow()

    private val _selectedRecentOperations = MutableStateFlow<Set<String>>(emptySet())
    val selectedRecentOperations: StateFlow<Set<String>> = _selectedRecentOperations.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _localOperations = MutableStateFlow<List<RecentOperation>>(emptyList())
    override val localOperations: StateFlow<List<RecentOperation>> = _localOperations.asStateFlow()

    private val _callLogs = MutableStateFlow<List<RecentOperation>>(emptyList())
    override val callLogs: StateFlow<List<RecentOperation>> = _callLogs.asStateFlow()
    private val _isRefreshingCallLogs = MutableStateFlow(false)

    override val topUsedContacts = contactUsageRepository.getTopUsedContacts(10)

    private val _isLoadingMoreCallLogs = MutableStateFlow(false)
    val isLoadingMoreCallLogs: StateFlow<Boolean> = _isLoadingMoreCallLogs.asStateFlow()

    private val _hasMoreCallLogs = MutableStateFlow(true)
    val hasMoreCallLogs: StateFlow<Boolean> = _hasMoreCallLogs.asStateFlow()

    private val callLogCache = mutableListOf<RecentOperation>()
    private var callLogOldestTimestamp: Long? = null

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()
    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult.asStateFlow()

    private val _accounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val accounts: StateFlow<List<ContactAccount>> = _accounts.asStateFlow()

    private val _blockedNumbers = MutableStateFlow<List<BlockedNumberInfo>>(emptyList())
    val blockedNumbers: StateFlow<List<BlockedNumberInfo>> = _blockedNumbers.asStateFlow()

    data class SnackEvent(
        val message: String,
        val kind: SnackbarType = SnackbarType.Info,
        val actionLabel: String? = null
    )

    private val _snackbarEvents = MutableSharedFlow<SnackEvent>(extraBufferCapacity = 1)
    val snackbarEvents = _snackbarEvents.asSharedFlow()
    private val pendingSnackbarStore = PendingSnackbarStore()
    val pendingSnackbarEvents = pendingSnackbarStore.events
    private val contactMediaCoordinator = ContactMediaCoordinator(
        application,
        contactRepository,
        viewModelScope,
    ) { resourceId ->
        pendingSnackbarStore.enqueue(
            listOf(SnackEvent(localizedContext().getString(resourceId), SnackbarType.Warning)),
        )
    }

    val vCard = ContactVCardCoordinator(
        repository = contactVCardRepository,
        scope = viewModelScope,
        currentContacts = { _contacts.value },
        setImporting = { importing -> _isImporting.value = importing },
        onContactsImported = ::loadContacts,
    )

    fun consumePendingSnackbarEvents(events: List<SnackEvent>) {
        pendingSnackbarStore.consume(events)
    }

    private val providerObserver = ContactsProviderObserver(application.contentResolver)

    init {
        viewModelScope.launch {
            recentOperationsRepository.getLocalOperations().collect { ops ->
                _localOperations.value = ops
            }
        }
        viewModelScope.launch {
            contactCacheRepository.observeCachedContacts().collect { cachedContacts ->
                _contacts.value = cachedContacts
            }
        }
        viewModelScope.launch {
            contactCacheRepository.observeCachedProfile().collect { cachedProfile ->
                _profile.value = cachedProfile
            }
        }
        providerObserver.start(viewModelScope) { refreshContactsFromSource() }
    }

    private val actors =
        java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.channels.Channel<UpdateRequest>>()

    private fun getOrCreateActor(
        key: String,
        worker: suspend (UpdateRequest) -> WorkerResult
    ): kotlinx.coroutines.channels.SendChannel<UpdateRequest> {
        return actors.getOrPut(key) {
            val ch =
                kotlinx.coroutines.channels.Channel<UpdateRequest>(capacity = kotlinx.coroutines.channels.Channel.CONFLATED)

            viewModelScope.launch(Dispatchers.IO) {
                for (first in ch) {
                    var req = first
                    delay(120.milliseconds)
                    while (true) {
                        val next = ch.tryReceive().getOrNull() ?: break
                        req = next
                    }

                    try {
                        val result = worker(req)
                        if (result.error == null) {
                            withContext(Dispatchers.Main) {
                                val savedContact = result.contactFor(req)
                                if (req.isProfile) _profile.value = savedContact
                                _contacts.value = _contacts.value.map {
                                    if (it.id == savedContact.id || it.id == req.desired.id) {
                                        savedContact
                                    } else {
                                        it
                                    }
                                }
                                pendingSnackbarStore.enqueueSaveResult(
                                    successMessage = req.successMessage,
                                    photoWarningMessage = localizedContext().getString(
                                        R.string.contact_photo_save_warning,
                                    ).takeIf { result.photoWarning },
                                )
                                req.onResult?.invoke(
                                    if (result.photoWarning) {
                                        ContactSaveResult.SuccessWithPhotoWarning(savedContact)
                                    } else {
                                        ContactSaveResult.Success(savedContact)
                                    },
                                )
                                refreshWidgets()
                            }
                            req.reconcile()
                        } else {
                            withContext(Dispatchers.Main) {
                                req.previousSnapshot?.let { prev ->
                                    viewModelScope.launch {
                                        req.rollback(
                                            prev
                                        )
                                    }
                                }
                                val message = req.errorMessage.ifBlank {
                                    localizedContext().getString(R.string.edit_contact_error)
                                }
                                _snackbarEvents.tryEmit(SnackEvent(message, SnackbarType.Error))
                                req.onResult?.invoke(ContactSaveResult.Failure)
                            }
                        }
                    } catch (_: Throwable) {
                        DiagnosticLog.error(DiagnosticEvent.CONTACT_ACTOR_FAILED)
                        withContext(Dispatchers.Main) {
                            req.previousSnapshot?.let { prev ->
                                viewModelScope.launch {
                                    req.rollback(
                                        prev
                                    )
                                }
                            }
                            val message = req.errorMessage.ifBlank {
                                localizedContext().getString(R.string.edit_contact_error)
                            }
                            _snackbarEvents.tryEmit(SnackEvent(message, SnackbarType.Error))
                            req.onResult?.invoke(ContactSaveResult.Failure)
                        }
                    }
                }
            }

            ch
        }
    }

    data class PreloadResult(val usedCache: Boolean)

    suspend fun preloadContacts(): PreloadResult {
        _isLoading.value = true
        return try {
            val cachedContacts = contactCacheRepository.getCachedContacts()
            val cachedProfile = contactCacheRepository.getCachedProfile()
            val usedCache = cachedContacts.isNotEmpty()
            if (usedCache) {
                _contacts.value = cachedContacts
                _profile.value = cachedProfile
            }

            if (usedCache) {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { refreshContactsFromSource() }
                        .onFailure { DiagnosticLog.error(DiagnosticEvent.CONTACT_REFRESH_FAILED) }
                }
            } else {
                refreshContactsFromSource()
            }

            PreloadResult(usedCache)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CONTACT_PRELOAD_FAILED)
            PreloadResult(usedCache = false)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadAdditionalData() {
        try {
            fetchAllRecentOperations()
            _accounts.value = contactRepository.getAccounts()
            loadBlockedNumbersInternal()
            contactRecycleBin.purgeExpired(System.currentTimeMillis())
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CONTACT_ADDITIONAL_DATA_LOAD_FAILED)
        }
    }

    private suspend fun refreshContactsFromSource(): List<Contact> {
        providerObserver.register()
        val contacts = contactRepository.getContacts()
        val profile = getProfileContact()
        contactCacheRepository.replaceCachedContacts(contacts, profile)
        _contacts.value = contacts
        _profile.value = profile
        loadBlockedNumbersInternal()
        return contacts
    }

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                refreshContactsFromSource()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createContact(
        contact: Contact,
        account: ContactAccount,
        successMessage: String = "",
        favorite: Boolean? = null,
        onSaveResult: ((ContactSaveResult) -> Unit)? = null,
        onResult: ((Contact?) -> Unit)? = null
    ) {
        val sanitizedInput = ContactInputPolicy.sanitize(contact)
        val contactWithAccount = sanitizedInput.copy(accounts = listOf(account))
        if (contactWithAccount.phones.isEmpty()) {
            val errorMessage = localizedContext().getString(R.string.add_contact_error)
            _snackbarEvents.tryEmit(SnackEvent(message = errorMessage, kind = SnackbarType.Error))
            onSaveResult?.invoke(ContactSaveResult.Failure)
            onResult?.invoke(null)
            return
        }

        val tempId = System.currentTimeMillis().toString()
        val optimisticContact = contactWithAccount.copy(id = tempId)
        val previousContacts = _contacts.value
        _contacts.value = previousContacts + optimisticContact
        val errorMessage = localizedContext().getString(R.string.add_contact_error)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestedPhoto = contactWithAccount.photoUri
                val preparedPhoto = requestedPhoto?.let { uri ->
                    preparePhotoAndCleanup(uri)
                }

                val inserted = contactRepository.insertContact(contactWithAccount, account, favorite)

                if (inserted) {
                    funnelTelemetry.firstContactAction(ContactAction.CREATE)
                    val refreshedContacts = refreshContactsFromSource()
                    val resolved = ContactInputPolicy.findMatch(
                        refreshedContacts,
                        contactWithAccount,
                    )
                    val photoSaved = when {
                        requestedPhoto == null -> true
                        preparedPhoto == null || resolved == null -> false
                        else -> {
                            val api = Contacts(application)
                            val entity = api.query()
                                .where { Contact.Id equalTo resolved.id }
                                .find()
                                .firstOrNull()
                            entity != null && writePreparedPhoto(entity, api, preparedPhoto)
                        }
                    }
                    val finalContact = if (requestedPhoto != null && resolved != null) {
                        refreshContactsFromSource().firstOrNull { it.id == resolved.id } ?: resolved
                    } else {
                        resolved ?: contactWithAccount
                    }

                    withContext(Dispatchers.Main) {
                        if (successMessage.isNotEmpty()) {
                            Toast.makeText(
                                application,
                                successMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (!photoSaved) emitPhotoWarning()
                        onSaveResult?.invoke(
                            if (photoSaved) {
                                ContactSaveResult.Success(finalContact)
                            } else {
                                ContactSaveResult.SuccessWithPhotoWarning(finalContact)
                            },
                        )
                        onResult?.invoke(finalContact)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _contacts.value = previousContacts
                        _snackbarEvents.tryEmit(
                            SnackEvent(message = errorMessage, kind = SnackbarType.Error)
                        )
                        onSaveResult?.invoke(ContactSaveResult.Failure)
                        onResult?.invoke(null)
                    }
                }
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.CONTACT_CREATE_FAILED)
                withContext(Dispatchers.Main) {
                    _contacts.value = previousContacts
                    _snackbarEvents.tryEmit(
                        SnackEvent(message = errorMessage, kind = SnackbarType.Error)
                    )
                    onSaveResult?.invoke(ContactSaveResult.Failure)
                    onResult?.invoke(null)
                }
            }
        }
    }

    fun createProfile(
        contact: Contact,
        successMessage: String = "",
        onSaveResult: ((ContactSaveResult) -> Unit)? = null,
        onSuccess: ((Contact) -> Unit)? = null
    ) {
        val sanitizedContact = ContactInputPolicy.sanitize(contact)
        val tempProfile = sanitizedContact.copy(id = "temp_profile_${System.currentTimeMillis()}")
        _profile.value = tempProfile

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestedPhoto = sanitizedContact.photoUri
                val preparedPhoto = requestedPhoto?.let { uri ->
                    preparePhotoAndCleanup(uri)
                }

                val insertResult = Contacts(application)
                    .profile()
                    .insertWithPermission()
                    .rawContact {
                        setName {
                            displayName = sanitizedContact.name
                        }

                        sanitizedContact.phones.forEach { phone ->
                            addPhone {
                                number = phone.number
                                type = phone.type
                            }
                        }

                        if (!sanitizedContact.email.isNullOrBlank()) {
                            addEmail {
                                address = sanitizedContact.email
                                type = EmailEntity.Type.WORK
                            }
                        }

                        if (!sanitizedContact.company.isNullOrBlank()) {
                            setOrganization {
                                company = sanitizedContact.company
                            }
                        }

                        val contactBirthday = sanitizedContact.birthday
                        if (contactBirthday != null) {
                            addEvent {
                                type = EventEntity.Type.BIRTHDAY
                                date = ContactUtils.longToEventDate(contactBirthday)
                            }
                        }

                        if (sanitizedContact.socialMedia.isNotEmpty()) {
                            removeAllWebsites()
                            sanitizedContact.socialMedia.forEach { socialNetwork ->
                                addWebsite {
                                    url = socialNetwork
                                }
                            }
                        } else {
                            removeAllWebsites()
                        }

                    }.commit()

                if (insertResult.isSuccessful) {
                    val photoSaved = when {
                        requestedPhoto == null -> true
                        preparedPhoto == null -> false
                        else -> {
                            val api = Contacts(application)
                            val entity = api.profile().query().find().contact
                            entity != null && writePreparedPhoto(entity, api, preparedPhoto)
                        }
                    }
                    val finalProfile = profileRepository.getProfileContact()

                    withContext(Dispatchers.Main) {
                        _profile.value = finalProfile

                        if (successMessage.isNotEmpty()) {
                            Toast.makeText(application, successMessage, Toast.LENGTH_SHORT).show()
                        }
                        if (!photoSaved) emitPhotoWarning()
                        finalProfile?.let { profile ->
                            onSaveResult?.invoke(
                                if (photoSaved) {
                                    ContactSaveResult.Success(profile)
                                } else {
                                    ContactSaveResult.SuccessWithPhotoWarning(profile)
                                },
                            )
                        } ?: onSaveResult?.invoke(ContactSaveResult.Failure)

                        finalProfile?.let { profile ->
                            onSuccess?.invoke(profile)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _profile.value = null
                        Toast.makeText(
                            application,
                            localizedContext().getString(R.string.profile_create_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        onSaveResult?.invoke(ContactSaveResult.Failure)
                    }
                }
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.CONTACT_PROFILE_CREATE_FAILED)
                withContext(Dispatchers.Main) {
                    _profile.value = null
                    Toast.makeText(
                        application,
                        localizedContext().getString(R.string.profile_create_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSaveResult?.invoke(ContactSaveResult.Failure)
                }
            }
        }
    }

    fun uriToBitmap(uri: Uri): Bitmap? = contactMediaCoordinator.uriToBitmap(uri)

    override suspend fun loadContactPhoto(uri: Uri): Bitmap? = contactMediaCoordinator.loadContactPhoto(uri)

    suspend fun getIncomingCallBackgroundUri(contactId: String): Uri? = contactMediaCoordinator.getIncomingCallBackgroundUri(contactId)
    fun updateIncomingCallBackground(contactId: String, change: IncomingCallBackgroundChange, onResult: (Boolean) -> Unit) =
        contactMediaCoordinator.updateIncomingCallBackground(contactId, change, onResult)
    suspend fun getInCallBackgroundUri(contactId: String): Uri? = contactMediaCoordinator.getInCallBackgroundUri(contactId)
    fun updateInCallBackground(contactId: String, change: InCallBackgroundChange, onResult: (Boolean) -> Unit) =
        contactMediaCoordinator.updateInCallBackground(contactId, change, onResult)

    override fun addRecentOperation(contact: Contact, type: RecentType, target: String) {
        viewModelScope.launch {
            recentOperationsRepository.addRecentOperation(
                RecentOperation(
                    contact = contact,
                    date = System.currentTimeMillis(),
                    type = type,
                    target = target
                )
            )
            fetchAllRecentOperations()
        }
    }

    fun deleteSelectedRecentOperations() {
        val toDelete = _selectedRecentOperations.value
        if (toDelete.isEmpty()) return
        val selectedOps = (_localOperations.value + _callLogs.value).filter { it.historyKey in toDelete }
        deleteRecentOperations(selectedOps)
    }

    fun deleteRecentOperations(operations: List<RecentOperation>) {
        val ids = operations.idsBySource()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (ids.callLogIds.isNotEmpty()) {
                    deleteCallLogs(ids.callLogIds)
                }
                if (ids.localIds.isNotEmpty()) {
                    recentOperationsRepository.deleteRecentOperationsByIds(ids.localIds)
                }
            }
            fetchAllRecentOperations()
            clearRecentOperationSelection()
        }
    }

    override fun fetchAllRecentOperations() {
        DiagnosticLog.debug(DiagnosticEvent.CONTACT_RECENTS_REFRESHED)
        if (!PermissionUtils.hasCallLogPermission(application)) {
            callLogCache.clear()
            callLogOldestTimestamp = null
            _callLogs.value = emptyList()
            _hasMoreCallLogs.value = false
            return
        }

        viewModelScope.launch {
            if (_isRefreshingCallLogs.value) return@launch
            _isRefreshingCallLogs.value = true
            try {
                val contactsSnapshot = currentContactsSnapshot()
                val callLogsResult = withContext(Dispatchers.IO) {
                    getCallLogs(
                        limit = CALL_LOG_PAGE_SIZE,
                        olderThan = null,
                        contactsSnapshot = contactsSnapshot
                    )
                }
                callLogCache.clear()
                callLogCache.addAll(callLogsResult)
                callLogOldestTimestamp = callLogCache.minOfOrNull { it.date }
                _hasMoreCallLogs.value = callLogsResult.size >= CALL_LOG_PAGE_SIZE
                _callLogs.value = callLogCache.toList()

                if (_localOperations.value.isEmpty()) {
                    val localOpsResult = withContext(Dispatchers.IO) {
                        recentOperationsRepository.getLocalOperations().first()
                    }
                    _localOperations.value = localOpsResult
                }
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.CONTACT_RECENTS_LOAD_FAILED)
            } finally {
                _isRefreshingCallLogs.value = false
            }
        }
    }

    fun loadMoreRecentOperations() {
        if (!PermissionUtils.hasCallLogPermission(application)) return
        val anchor = callLogOldestTimestamp ?: return
        if (_isLoadingMoreCallLogs.value || !_hasMoreCallLogs.value) return

        viewModelScope.launch {
            _isLoadingMoreCallLogs.value = true
            try {
                val contactsSnapshot = currentContactsSnapshot()
                val callLogsResult = withContext(Dispatchers.IO) {
                    getCallLogs(
                        limit = CALL_LOG_PAGE_SIZE,
                        olderThan = anchor,
                        contactsSnapshot = contactsSnapshot
                    )
                }
                if (callLogsResult.isNotEmpty()) {
                    val merged = (callLogCache + callLogsResult)
                        .distinctBy { it.historyKey }
                        .sortedByDescending { it.date }
                    callLogCache.clear()
                    callLogCache.addAll(merged)
                    callLogOldestTimestamp = callLogCache.minOfOrNull { it.date }
                    _callLogs.value = callLogCache.toList()
                }
                _hasMoreCallLogs.value = callLogsResult.size >= CALL_LOG_PAGE_SIZE
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.CONTACT_RECENTS_LOAD_MORE_FAILED)
            } finally {
                _isLoadingMoreCallLogs.value = false
            }
        }
    }

    fun deleteContact(
        contactId: String,
        successMessage: String? = null,
        errorMessage: String? = null,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val currentContacts = _contacts.value
        val index = currentContacts.indexOfFirst { it.id == contactId }
        if (index == -1) {
            val fallback = errorMessage ?: localizedContext().getString(R.string.delete_contact_error)
            _snackbarEvents.tryEmit(SnackEvent(fallback, SnackbarType.Error))
            onResult?.invoke(false)
            return
        }

        val ids = listOfNotNull(contactId.toLongOrNull())
        if (ids.isEmpty()) {
            val fallback = errorMessage ?: localizedContext().getString(R.string.delete_contact_error)
            _snackbarEvents.tryEmit(SnackEvent(fallback, SnackbarType.Error))
            onResult?.invoke(false)
            return
        }

        val mutableContacts = currentContacts.toMutableList().apply { removeAt(index) }
        _contacts.value = mutableContacts

        viewModelScope.launch(Dispatchers.IO) {
            val deleted = contactRecycleBin.deleteToBin(listOf(currentContacts[index]))
            if (deleted) {
                funnelTelemetry.firstContactAction(ContactAction.DELETE)
                recentOperationsRepository.deleteRecentOperations(ids)
            }
            withContext(Dispatchers.Main) {
                if (deleted) {
                    if (!successMessage.isNullOrBlank()) {
                        Toast.makeText(
                            application,
                            successMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    loadContacts()
                    onResult?.invoke(true)
                } else {
                    _contacts.value = currentContacts
                    val fallback = errorMessage ?: localizedContext().getString(R.string.delete_contact_error)
                    _snackbarEvents.tryEmit(SnackEvent(fallback, SnackbarType.Error))
                    onResult?.invoke(false)
                }
            }
        }
    }

    fun deleteProfile(profileId: Long, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val deleted = profileRepository.deleteProfile()
            if (deleted) {
                recentOperationsRepository.deleteRecentOperations(listOf(profileId))
            }
            _profile.value = getProfileContact()
            onDeleted?.invoke()
        }
    }

    fun deleteSelectedContacts(
        successMessage: String? = null,
        errorMessage: String? = null,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val toDelete = _selectedContacts.value
        if (toDelete.isEmpty()) return

        val previousContacts = _contacts.value
        val remaining = previousContacts.filterNot { toDelete.contains(it.id) }
        _contacts.value = remaining
        _isDeleting.value = true

        val ids = toDelete.mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) {
            _contacts.value = previousContacts
            _isDeleting.value = false
            val fallback = errorMessage ?: localizedContext().getString(R.string.delete_contacts_error)
            _snackbarEvents.tryEmit(SnackEvent(fallback, SnackbarType.Error))
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val deleted = contactRecycleBin.deleteToBin(
                previousContacts.filter { toDelete.contains(it.id) },
            )
            if (deleted) {
                funnelTelemetry.firstContactAction(ContactAction.DELETE)
                recentOperationsRepository.deleteRecentOperations(ids)
            }
            withContext(Dispatchers.Main) {
                _isDeleting.value = false
                if (deleted) {
                    loadContacts()
                    clearContactSelection()
                    if (!successMessage.isNullOrBlank()) {
                        _snackbarEvents.tryEmit(
                            SnackEvent(successMessage, SnackbarType.Success)
                        )
                    }
                    onResult?.invoke(true)
                } else {
                    _contacts.value = previousContacts
                    val fallback = errorMessage ?: localizedContext().getString(R.string.delete_contacts_error)
                    _snackbarEvents.tryEmit(SnackEvent(fallback, SnackbarType.Error))
                    onResult?.invoke(false)
                }
            }
        }
    }

    fun importContacts(
        jsonUri: Uri,
        contactsViewModel: ContactsViewModel,
        account: ContactAccount
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _importResult.value = null
            try {
                val importedCount = contactImportRepository.importContactsFromJson(
                    jsonUri,
                    contactsViewModel,
                    account
                )
                _importResult.value = importedCount
                loadContacts()
            } catch (_: Exception) {
                _importResult.value = 0
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun toggleFavorite(contactId: String, message: String) {
        val currentFavoriteStatus = _contacts.value
            .firstOrNull { it.id == contactId }
            ?.isFavorite
            ?: return
        setFavorite(contactId, !currentFavoriteStatus, message)
    }

    fun setFavorite(contactId: String, isFavorite: Boolean, message: String) {
        val previousContact = _contacts.value.firstOrNull { it.id == contactId } ?: return
        if (previousContact.isFavorite == isFavorite) return

        _contacts.value = _contacts.value.map { c ->
            if (c.id == contactId) c.copy(isFavorite = isFavorite) else c
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contactsApi = Contacts(application)
                val contact = contactsApi
                    .query()
                    .where { Contact.Id equalTo contactId }
                    .find()
                    .firstOrNull()

                if (contact == null) {
                    withContext(Dispatchers.Main) {
                        _contacts.value = _contacts.value.map { c ->
                            if (c.id == contactId) previousContact else c
                        }
                        _snackbarEvents.tryEmit(
                            SnackEvent(
                                message = localizedContext().getString(R.string.contact_edit_not_found),
                                kind = SnackbarType.Error
                            )
                        )
                    }
                    return@launch
                }

                val updateResult = contactsApi
                    .update()
                    .contacts(
                        contact.mutableCopy {
                            setOptions {
                                starred = isFavorite
                            }
                        }
                    )
                    .deleteBlanks(true)
                    .commit()

                if (updateResult.isSuccessful) {
                    val refreshedContacts = refreshContactsFromSource()
                    val persistedFavorite = refreshedContacts.firstOrNull {
                        it.id == contactId
                    }?.isFavorite

                    if (persistedFavorite == isFavorite) {
                        withContext(Dispatchers.Main) {
                            if (message.isNotEmpty()) {
                                _snackbarEvents.tryEmit(
                                    SnackEvent(
                                        message = message,
                                        kind = SnackbarType.Success
                                    )
                                )
                            }
                            refreshWidgets()
                        }
                    } else {
                        showFavoriteUpdateError(contactId, previousContact)
                    }
                } else {
                    showFavoriteUpdateError(contactId, previousContact)
                }
            } catch (_: Exception) {
                showFavoriteUpdateError(contactId, previousContact)
            }
        }
    }

    private suspend fun showFavoriteUpdateError(contactId: String, previousContact: Contact) {
        withContext(Dispatchers.Main) {
            _contacts.value = _contacts.value.map { contact ->
                if (contact.id == contactId) previousContact else contact
            }
            _snackbarEvents.tryEmit(
                SnackEvent(
                    message = localizedContext().getString(R.string.contact_update_error),
                    kind = SnackbarType.Error
                )
            )
        }
    }

    fun blockContact(contact: Contact) {
        viewModelScope.launch {
            val contactId = contact.id
            val previousState = _contacts.value.find { it.id == contactId }?.isBlocked ?: contact.isBlocked
            if (!previousState) {
                updateBlockedState(contactId, true)
            }

            val firstName = ContactUtils.getFirstName(contact.name)
            val blockResult = runCatching { contactRepository.blockContact(contact) }
            val success = blockResult.getOrDefault(false)

            if (success) {
                refreshBlockedNumbers()
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.contact_block_success, firstName),
                        kind = SnackbarType.Success
                    )
                )
            } else {
                if (!previousState) {
                    updateBlockedState(contactId, previousState)
                }
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.contact_block_error),
                        kind = SnackbarType.Error
                    )
                )
            }
        }
    }

    fun unblockContact(contact: Contact) {
        viewModelScope.launch {
            val contactId = contact.id
            val previousState = _contacts.value.find { it.id == contactId }?.isBlocked ?: contact.isBlocked
            if (previousState) {
                updateBlockedState(contactId, false)
            }

            val firstName = ContactUtils.getFirstName(contact.name)
            val unblockResult = runCatching { contactRepository.unblockContact(contact) }
            val success = unblockResult.getOrDefault(false)

            if (success) {
                refreshBlockedNumbers()
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.contact_unblock_success, firstName),
                        kind = SnackbarType.Success
                    )
                )
            } else {
                if (previousState) {
                    updateBlockedState(contactId, previousState)
                }
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.contact_unblock_error),
                        kind = SnackbarType.Error
                    )
                )
            }
        }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            val success = runCatching { contactRepository.blockNumber(number) }.getOrDefault(false)
            if (success) refreshBlockedNumbers()
            _snackbarEvents.tryEmit(
                SnackEvent(
                    message = localizedContext().getString(
                        if (success) R.string.blocked_number_added else R.string.blocked_number_add_error,
                    ),
                    kind = if (success) SnackbarType.Success else SnackbarType.Error,
                )
            )
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            val success = contactRepository.unblockNumber(number)
            if (success) {
                refreshBlockedNumbers()
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.blocked_number_unblocked),
                        kind = SnackbarType.Success
                    )
                )
            } else {
                _snackbarEvents.tryEmit(
                    SnackEvent(
                        message = localizedContext().getString(R.string.blocked_number_unblock_error),
                        kind = SnackbarType.Error
                    )
                )
            }
        }
    }

    private fun updateBlockedState(contactId: String, isBlocked: Boolean) {
        _contacts.value = _contacts.value.map { contact ->
            if (contact.id == contactId) contact.copy(isBlocked = isBlocked) else contact
        }
        _profile.value = _profile.value?.let { profile ->
            if (profile.id == contactId) profile.copy(isBlocked = isBlocked) else profile
        }
    }

    private suspend fun loadBlockedNumbersInternal() {

        val combinedContacts = buildList {
            addAll(_contacts.value)
            _profile.value?.let { add(it) }
        }

        val blockedInfos = contactRepository.getBlockedNumberInfos(combinedContacts)
        _blockedNumbers.value = blockedInfos

        val blockedIds = blockedInfos.mapNotNull { it.contact?.id }.toSet()
        _contacts.value = _contacts.value.map { contact ->
            val blocked = blockedIds.contains(contact.id)
            if (contact.isBlocked == blocked) contact else contact.copy(isBlocked = blocked)
        }
        _profile.value = _profile.value?.let { profile ->
            val blocked = blockedIds.contains(profile.id)
            if (profile.isBlocked == blocked) profile else profile.copy(isBlocked = blocked)
        }
    }

    private fun refreshBlockedNumbers() {
        viewModelScope.launch {
            loadBlockedNumbersInternal()
        }
    }

    fun toggleContactSelection(contactId: String) {
        _selectedContacts.value = _selectedContacts.value.toMutableSet().apply {
            if (contains(contactId)) remove(contactId) else add(contactId)
        }
    }

    fun clearContactSelection() {
        _selectedContacts.value = emptySet()
    }

    fun selectAllContacts(list: List<Contact>) {
        _selectedContacts.value = list.map { it.id }.toSet()
    }

    fun toggleRecentOperationSelection(operation: RecentOperation) {
        val key = operation.historyKey
        _selectedRecentOperations.value = _selectedRecentOperations.value.toMutableSet().apply {
            if (contains(key)) remove(key) else add(key)
        }
    }

    fun clearRecentOperationSelection() {
        _selectedRecentOperations.value = emptySet()
    }

    fun selectAllRecentOperations(list: List<RecentOperation>) {
        _selectedRecentOperations.value = list.map { it.historyKey }.toSet()
    }

    override suspend fun findContactByPhoneNumber(phoneNumber: String): Contact? {
        val allContacts = ArrayList(contacts.value)
        profile.value?.let { allContacts.add(0, it) }
        return contactRepository.findContactByPhoneNumber(allContacts, phoneNumber)
    }

    fun findContactById(contactId: String): Contact? {
        return contacts.value.find { it.id == contactId }
    }

    fun findContactByPhones(phones: List<ContactPhone>): Contact? {
        if (phones.isEmpty()) return null
        val targets = phones.mapNotNull {
            ContactInputPolicy.normalizePhoneForLookup(it.number)
        }.toSet()
        if (targets.isEmpty()) return null
        return contacts.value.firstOrNull { candidate ->
            candidate.phones.any {
                ContactInputPolicy.normalizePhoneForLookup(it.number) in targets
            }
        }
    }

    suspend fun getProfileContact(): Contact? {
        return profileRepository.getProfileContact()
    }

    fun updateContact(
        contact: Contact,
        successMessage: String,
        onResult: ((ContactSaveResult) -> Unit)? = null,
    ) {
        val sanitizedContact = ContactInputPolicy.sanitize(contact)
        val previous = _contacts.value.find { it.id == sanitizedContact.id }
        val errorMessage = localizedContext().getString(R.string.edit_contact_error)
        val photoChanged =
            previous?.photoUri?.toString() != sanitizedContact.photoUri?.toString()

        val desired = sanitizedContact.copy(
            accounts = previous?.accounts ?: sanitizedContact.accounts,
            isFavorite = previous?.isFavorite ?: sanitizedContact.isFavorite,
            remotePhotoUri = if (photoChanged) null else sanitizedContact.remotePhotoUri
        )

        if (desired.phones.isEmpty()) {
            _snackbarEvents.tryEmit(SnackEvent(errorMessage, SnackbarType.Error))
            onResult?.invoke(ContactSaveResult.Failure)
            return
        }

        funnelTelemetry.firstContactAction(ContactAction.UPDATE)
        _contacts.value = _contacts.value.map { if (it.id == desired.id) desired else it }

        val key = actorKey(desired.id, isProfile = false)
        getOrCreateActor(
            key = key,
            worker = ::contactWorker,
        ).trySend(
            UpdateRequest(
                key = key,
                desired = desired,
                successMessage = successMessage,
                errorMessage = errorMessage,
                previousSnapshot = previous,
                rollback = { prev ->
                    _contacts.value = _contacts.value.map { if (it.id == prev.id) prev else it }
                },
                reconcile = {
                    viewModelScope.launch(Dispatchers.IO) {
                        if (photoChanged) delay(700.milliseconds)
                        val fresh = runCatching { findContactById(desired.id) }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (fresh != null) {
                                _contacts.value =
                                    _contacts.value.map { if (it.id == fresh.id) fresh else it }
                                if (_profile.value?.id == fresh.id) _profile.value = fresh
                            }
                        }
                        loadContacts()
                    }
                },
                onResult = onResult,
            )
        )
    }

    fun updateProfile(
        contact: Contact,
        successMessage: String = "",
        onResult: ((ContactSaveResult) -> Unit)? = null,
    ) {
        val sanitizedContact = ContactInputPolicy.sanitize(contact)
        val previous = _profile.value
        val errorMessage = localizedContext().getString(R.string.edit_contact_error)
        val photoChanged =
            previous?.photoUri?.toString() != sanitizedContact.photoUri?.toString()

        val optimistic = previous?.let {
            sanitizedContact.copy(
                id = it.id,
                accounts = it.accounts,
                isFavorite = it.isFavorite,
                remotePhotoUri = if (photoChanged) null else sanitizedContact.remotePhotoUri
            )
        } ?: sanitizedContact.copy(
            remotePhotoUri = if (photoChanged) null else sanitizedContact.remotePhotoUri
        )

        if (optimistic.phones.isEmpty()) {
            _snackbarEvents.tryEmit(SnackEvent(errorMessage, SnackbarType.Error))
            onResult?.invoke(ContactSaveResult.Failure)
            return
        }

        _profile.value = optimistic
        _contacts.value = _contacts.value.map { if (it.id == optimistic.id) optimistic else it }

        val key = actorKey(previous?.id ?: optimistic.id, isProfile = true)
        getOrCreateActor(key, ::profileWorker).trySend(
            UpdateRequest(
                key = key,
                desired = optimistic,
                successMessage = successMessage,
                errorMessage = errorMessage,
                previousSnapshot = previous,
                rollback = { prev ->
                    _profile.value = prev
                    _contacts.value = _contacts.value.map { if (it.id == prev.id) prev else it }
                },
                reconcile = {
                    viewModelScope.launch(Dispatchers.IO) {
                        if (photoChanged) delay(700.milliseconds)
                        runCatching { profileRepository.getProfileContact() }.getOrNull()?.let { fresh ->
                            withContext(Dispatchers.Main) {
                                _profile.value = fresh
                            }
                        }
                        loadContacts()
                    }
                },
                onResult = onResult,
            )
        )
    }

    suspend fun shareContactQR(
        contact: Contact,
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedContact = ContactInputPolicy.sanitize(contact)
                val contactMap = mutableMapOf<String, Any?>()
                contactMap["name"] = sanitizedContact.name
                contactMap["phones"] = sanitizedContact.phones
                contactMap["birthday"] = sanitizedContact.birthday
                contactMap["email"] = sanitizedContact.email
                contactMap["company"] = sanitizedContact.company
                contactMap["isFavorite"] = sanitizedContact.isFavorite
                contactMap["socialMedia"] = sanitizedContact.socialMedia
                contactMap["remotePhotoUri"] = null
                val gson = GsonBuilder().create()
                val contactJson = ContactUtils.CONTACT_OBJECT + gson.toJson(contactMap)
                val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
                hints[EncodeHintType.MARGIN] = 1
                hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
                hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
                val writer = QRCodeWriter()
                val bitMatrix =
                    writer.encode(contactJson, BarcodeFormat.QR_CODE, 512, 512, hints)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        pixels[y * width + x] =
                            if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    }
                }

                val bitmap = createBitmap(width, height)
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                bitmap
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.CONTACT_QR_GENERATION_FAILED)
                null
            }
        }
    }

    fun shareContactCard(contact: Contact, context: Context, includePhoto: Boolean = true) {
        viewModelScope.launch {
            val contactsApi = Contacts(application)
            val existingContact = contactsApi
                .query()
                .where { Contact.Id equalTo contact.id }
                .find()
                .firstOrNull()

            if (existingContact == null) {
                return@launch
            }

            val shareIntent = existingContact.shareVCardIntent(includePhoto = includePhoto)
            if (shareIntent != null) {
                context.startActivity(Intent.createChooser(shareIntent, null))
            }
        }
    }

    fun shareProfileCard(context: Context, includePhoto: Boolean = true) {
        viewModelScope.launch {
            val profileContact = Contacts(context).profile().query().find().contact!!

            val shareIntent = profileContact.shareVCardIntent(includePhoto = includePhoto)
            if (shareIntent != null) {
                context.startActivity(Intent.createChooser(shareIntent, null))
            }
        }
    }

    fun shareQRCode(bitmap: Bitmap, fileName: String = "contact_qr") {
        val cachePath = File(application.cacheDir, "images")
        cachePath.mkdirs()

        val file = File(cachePath, "${fileName}_${System.currentTimeMillis()}.png")

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            val contentUri = FileProvider.getUriForFile(
                application,
                application.packageName + ".provider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(
                shareIntent,
                localizedContext().getString(R.string.share_contact),
            )
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(chooserIntent)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CONTACT_QR_SHARE_FAILED)
            _snackbarEvents.tryEmit(
                SnackEvent(
                    message = localizedContext().getString(R.string.error_sharing_qr),
                    kind = SnackbarType.Error
                )
            )
        }
    }

    private suspend fun getCallLogs(
        limit: Int,
        olderThan: Long?,
        contactsSnapshot: List<Contact>
    ): List<RecentOperation> {
        val resolver: ContentResolver = application.contentResolver
        val queryUri = CallLog.Calls.CONTENT_URI.buildUpon()
            .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, limit.toString())
            .build()
        val selection = olderThan?.let { "${CallLog.Calls.DATE} < ?" }
        val selectionArgs = olderThan?.let { arrayOf(it.toString()) }
        val cache = mutableMapOf<String, Contact?>()

        val callLogs = mutableListOf<RecentOperation>()
        val cursor: Cursor? = resolver.query(
            queryUri,
            CALL_LOG_PROJECTION,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberIndex = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIndex = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIndex = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val typeIndex = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val rawNumber = c.getString(numberIndex)
                val number = rawNumber?.takeIf { it.isNotBlank() }
                val date = c.getLong(dateIndex)
                val duration = c.getLong(durationIndex)
                val typeInt = c.getInt(typeIndex)
                val type = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE -> RecentType.CALL_RECEIVED
                    CallLog.Calls.OUTGOING_TYPE -> RecentType.CALL_SENT
                    CallLog.Calls.MISSED_TYPE -> RecentType.CALL_MISSED
                    CallLog.Calls.REJECTED_TYPE -> RecentType.CALL_REJECTED
                    CallLog.Calls.BLOCKED_TYPE -> RecentType.CALL_BLOCKED
                    else -> RecentType.CALL_UNKNOWN
                }

                val resolvedNumber = number ?: localizedContext().getString(R.string.unknown)
                val resolvedContact = number?.let {
                    lookupContactByNumber(
                        contactsSnapshot = contactsSnapshot,
                        number = it,
                        cache = cache
                    )
                } ?: createUnknownContact(resolvedNumber)

                callLogs.add(
                    RecentOperation(
                        id = id,
                        contact = resolvedContact,
                        date = date,
                        type = type,
                        target = resolvedNumber,
                        time = duration,
                        source = RecentOperationSource.CALL_LOG,
                    )
                )
            }
        }

        return callLogs
    }

    private suspend fun lookupContactByNumber(
        contactsSnapshot: List<Contact>,
        number: String,
        cache: MutableMap<String, Contact?>
    ): Contact? {
        val keys = cacheKeysForNumber(number)
        keys.forEach { key ->
            if (cache.containsKey(key)) return cache[key]
        }

        val found = contactRepository.findContactByPhoneNumber(contactsSnapshot, number)
        keys.forEach { key -> cache[key] = found }
        return found
    }

    private fun cacheKeysForNumber(number: String): Set<String> {
        val keys = LinkedHashSet<String>()
        val trimmed = number.trim()
        if (trimmed.isNotEmpty()) keys.add(trimmed)

        val normalized = ContactInputPolicy.normalizePhoneForLookup(trimmed)
        normalized?.let { keys.add(it) }

        val digitsOnly = normalized?.filter { it.isDigit() }
            ?: trimmed.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty()) {
            keys.add(digitsOnly)
            if (digitsOnly.length > 10) keys.add(digitsOnly.takeLast(10))
            if (digitsOnly.length > 7) keys.add(digitsOnly.takeLast(7))
        }
        return keys
    }

    private fun createUnknownContact(number: String): Contact {
        return Contact(
            id = "",
            name = localizedContext().getString(R.string.unknown),
            phones = listOf(ContactPhone(number, PhoneEntity.Type.MOBILE)),
            email = null,
            company = null,
            photoUri = null,
            isFavorite = false
        )
    }

    private fun currentContactsSnapshot(): List<Contact> {
        val snapshot = ArrayList(_contacts.value)
        _profile.value?.let { snapshot.add(0, it) }
        return snapshot
    }

    fun deleteCallLogs(ids: List<Long>) {
        val resolver: ContentResolver = getApplication<Application>().contentResolver
        ids.forEach { id ->
            resolver.delete(
                CallLog.Calls.CONTENT_URI,
                "_id=?",
                arrayOf(id.toString())
            )
        }
    }

    private fun actorKey(id: String?, isProfile: Boolean): String {
        val safeId = id?.takeIf { it.isNotBlank() } ?: "unknown"
        return if (isProfile) "__PROFILE__:$safeId" else "__CONTACT__:$safeId"
    }

    companion object {
        private const val CALL_LOG_PAGE_SIZE = 150
        private val CALL_LOG_PROJECTION = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
    }

    private suspend fun contactWorker(req: UpdateRequest): WorkerResult {
        val api = Contacts(application)

        val existing = findContactEntity(api, req.desired.id)
            ?: resolveReaggregatedContactId(req)?.let { findContactEntity(api, it) }
            ?: return WorkerResult(error = "not_found")
        val resolvedId = existing.id.toString()
        val photoChanged = req.previousSnapshot?.photoUri?.toString() !=
            req.desired.photoUri?.toString()
        val preparedPhoto = if (photoChanged) {
            req.desired.photoUri?.let { uri ->
                preparePhotoAndCleanup(uri)
            }
        } else {
            null
        }

        val updated = existing.mutableCopy {
            applyEditableFields(req.desired)

            if (req.desired.notes.isNullOrBlank()) {
                setNote {
                    note = null
                }
            } else {
                setNote {
                    note = req.desired.notes
                }
            }

            setOptions {
                starred = req.desired.isFavorite
                customRingtone = req.desired.ringtone
            }
        }

        val commit = api.update().contacts(updated).commit()
        if (!commit.isSuccessful) return WorkerResult(error = "commit_failed")

        val photoSaved = when {
            !photoChanged -> true
            req.desired.photoUri == null -> removePhoto(existing, api)
            preparedPhoto == null -> false
            else -> writePreparedPhoto(existing, api, preparedPhoto)
        }
        val recentOps = _localOperations.value.filter { it.contact.id == req.desired.id }
        recentOps.forEach { recentOperationsRepository.updateRecentOperation(it.copy(contact = req.desired)) }

        val persistedContact = if (resolvedId == req.desired.id) {
            contactPhotoUpdateResolver.resolve(req, photoChanged)
        } else {
            runCatching {
                contactRepository.getContacts().firstOrNull { it.id == resolvedId }
            }.getOrNull() ?: req.desired.copy(id = resolvedId)
        }
        return WorkerResult(
            photoWarning = !photoSaved,
            persistedContact = persistedContact,
        )
    }

    private fun findContactEntity(api: Contacts, contactId: String) = api.query()
        .where { Contact.Id equalTo contactId }
        .find()
        .firstOrNull()

    private suspend fun resolveReaggregatedContactId(req: UpdateRequest): String? {
        val template = req.previousSnapshot ?: req.desired
        val candidates = runCatching { contactRepository.getContacts() }.getOrNull().orEmpty()
        return ContactInputPolicy.findUniquePhoneMatch(candidates, template)
            ?.id
            ?.takeIf { it != req.desired.id }
    }

    private suspend fun profileWorker(req: UpdateRequest): WorkerResult {
        val api = Contacts(application)

        val profileEntity = api.profile().query().find().contact
            ?: return WorkerResult(error = "not_found")
        val photoChanged = req.previousSnapshot?.photoUri?.toString() !=
            req.desired.photoUri?.toString()
        val preparedPhoto = if (photoChanged) {
            req.desired.photoUri?.let { uri ->
                preparePhotoAndCleanup(uri)
            }
        } else {
            null
        }

        val updated = profileEntity.mutableCopy {
            applyEditableFields(req.desired)

        }

        val commit = api.profile().update().apply { contact(updated) }.commit()
        if (!commit.isSuccessful) return WorkerResult(error = "commit_failed")

        val photoSaved = when {
            !photoChanged -> true
            req.desired.photoUri == null -> removePhoto(profileEntity, api)
            preparedPhoto == null -> false
            else -> writePreparedPhoto(profileEntity, api, preparedPhoto)
        }
        val persistedContact = contactPhotoUpdateResolver.resolve(req, photoChanged)
        return WorkerResult(
            photoWarning = !photoSaved,
            persistedContact = persistedContact,
        )
    }

    private fun writePreparedPhoto(
        contact: ExistingContactEntity,
        api: Contacts,
        preparedPhoto: PreparedContactPhoto,
    ): Boolean = contactMediaCoordinator.writeContactPhoto(contact, api, preparedPhoto)

    private fun removePhoto(contact: ExistingContactEntity, api: Contacts): Boolean =
        contactMediaCoordinator.removeContactPhoto(contact, api)

    private suspend fun preparePhotoAndCleanup(uri: Uri): PreparedContactPhoto? {
        return try {
            ImageProcessor.prepareContactPhoto(application, uri).getOrNull()
        } finally {
            uri.path?.let { path ->
                runCatching {
                    val file = File(path)
                    val cacheDirectory = application.cacheDir.canonicalFile
                    if (
                        file.canonicalFile.parentFile == cacheDirectory &&
                        (file.name.startsWith("crop_") || file.name.startsWith("contact_photo_"))
                    ) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun emitPhotoWarning() {
        _snackbarEvents.tryEmit(
            SnackEvent(
                message = localizedContext().getString(R.string.contact_photo_save_warning),
                kind = SnackbarType.Warning,
            ),
        )
    }

    override fun recordCallUsage(phoneNumber: String, duration: Long) {
        viewModelScope.launch {
            contactUsageRepository.recordCall(phoneNumber, duration)
        }
    }

    override fun onCleared() {
        super.onCleared()
        providerObserver.close()
        actors.values.forEach { it.close() }
        actors.clear()
        contactMediaCoordinator.evictContactPhotos()
    }
}
