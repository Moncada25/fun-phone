package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.ProfileRepository

internal data class UpdateRequest(
    val key: String,
    val desired: Contact,
    val successMessage: String,
    val errorMessage: String,
    val previousSnapshot: Contact?,
    val rollback: suspend (Contact) -> Unit,
    val reconcile: suspend () -> Unit,
    val onResult: ((ContactSaveResult) -> Unit)?,
)

internal val UpdateRequest.isProfile: Boolean
    get() = key.startsWith(PROFILE_ACTOR_KEY_PREFIX)

internal data class WorkerResult(
    val error: String? = null,
    val photoWarning: Boolean = false,
    val persistedContact: Contact? = null,
)

internal class ContactPhotoUpdateResolver(
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend fun resolve(request: UpdateRequest, photoChanged: Boolean): Contact? {
        if (!photoChanged) return null
        return runCatching {
            if (request.isProfile) {
                profileRepository.getProfileContact()
            } else {
                contactRepository.getContacts().firstOrNull { it.id == request.desired.id }
            }
        }.getOrNull()
    }
}

private const val PROFILE_ACTOR_KEY_PREFIX = "__PROFILE__:"

internal fun WorkerResult.contactFor(request: UpdateRequest): Contact =
    persistedContact ?: request.desired.copy(
        photoUri = if (
            request.previousSnapshot?.photoUri?.toString() == request.desired.photoUri?.toString()
        ) {
            request.desired.photoUri
        } else {
            request.previousSnapshot?.photoUri
        },
    )
