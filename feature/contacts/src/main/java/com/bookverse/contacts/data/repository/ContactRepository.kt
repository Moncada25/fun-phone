package com.bookverse.contacts.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.contacts.BlockedNumberMatchPolicy
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ImageProcessor
import contacts.core.Contacts
import contacts.core.ContactsFields
import contacts.core.Insert
import contacts.core.`in`
import contacts.core.asc
import contacts.core.equalTo
import contacts.core.entities.BlockedNumber
import contacts.core.entities.EmailEntity
import contacts.core.entities.EventEntity
import contacts.core.entities.NewBlockedNumber
import contacts.core.entities.Phone
import contacts.core.util.addEmail
import contacts.core.util.addEvent
import contacts.core.util.addPhone
import contacts.core.util.addWebsite
import contacts.core.util.contacts
import contacts.core.util.emails
import contacts.core.util.events
import contacts.core.util.noteList
import contacts.core.util.organizations
import contacts.core.util.phones
import contacts.core.util.setAccount
import contacts.core.util.setName
import contacts.core.util.setNote
import contacts.core.util.setOptions
import contacts.core.util.setOrganization
import contacts.core.util.websiteList
import contacts.permissions.accounts.queryWithPermission
import contacts.permissions.queryWithPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bookverse.contacts.core.ui.R as CoreUiR
import contacts.core.entities.Contact as ContactsCoreContact
import java.util.Locale

private const val MIN_DIGIT_MATCH = 7

class ContactRepository(private val context: Context) {
    private val contactPhotoStore = ContactPhotoStore(context)
    private val incomingCallBackgroundStore = IncomingCallBackgroundStore(context)
    private val inCallBackgroundStore = InCallBackgroundStore(context)

    suspend fun getAccounts(): List<ContactAccount> = withContext(Dispatchers.IO) {
        val accounts = Contacts(context).accounts().queryWithPermission().find().toList()

        val contactAccounts = mutableListOf<ContactAccount>()
        contactAccounts.add(ContactAccount.LOCAL_ACCOUNT)
        contactAccounts.addAll(accounts.map {
            ContactAccount.fromAccount(it)
        }.filterNot { ContactAccount.ACCOUNTS_BLACKLIST.contains(it.name) })

        DiagnosticLog.debug(DiagnosticEvent.CONTACT_ACCOUNTS_LOADED)

        contactAccounts
    }

    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsApi = Contacts(context)
        val allContactsRaw = contactsApi.queryWithPermission()
            .orderBy(ContactsFields.DisplayNamePrimary.asc(ignoreCase = true))
            .find()
        val blockedNumbers = contactsApi.queryBlockedNumbersIfSupported()
            .mapNotNull { sanitizeNumberForMatching(it.normalizedNumber ?: it.number) }
            .toSet()

        DiagnosticLog.debug(DiagnosticEvent.CONTACTS_LOADED)
        DiagnosticLog.debug(DiagnosticEvent.CONTACT_BLOCKED_NUMBERS_LOADED)

        val mappedContacts = allContactsRaw.mapNotNull { rawContact ->
            rawContact.toAppContact(
                unknownName = context.getString(CoreUiR.string.unknown),
                resolvedPhotoUri = contactPhotoStore.resolveContactPhoto(
                    lookupKey = rawContact.lookupKey,
                    providerUri = rawContact.photoUri,
                ),
            ).let { contact ->
                contact.copy(isBlocked = isContactBlocked(contact, blockedNumbers))
            }
        }.filter { it.phones.isNotEmpty() && it.phones.any { phone -> phone.number.isNotEmpty() } }
        if (contactsApi.permissions.canQuery()) {
            val activeLookupKeys = allContactsRaw.map { it.lookupKey }.toSet()
            contactPhotoStore.reconcileContacts(activeLookupKeys)
            incomingCallBackgroundStore.reconcile(activeLookupKeys)
            inCallBackgroundStore.reconcile(activeLookupKeys)
        }
        mappedContacts
    }

    suspend fun blockContact(contact: Contact): Boolean = withContext(Dispatchers.IO) {
        val candidates = contact.blockCandidates()
        if (candidates.isEmpty()) {
            DiagnosticLog.warning(DiagnosticEvent.CONTACT_BLOCK_SKIPPED_NO_NUMBER)
            return@withContext false
        }

        val contactsApi = Contacts(context)
        val existing = contactsApi.queryBlockedNumbersIfSupported()
        val existingNormalized =
            existing.mapNotNull { normalizeForComparison(it.normalizedNumber ?: it.number) }
                .toMutableSet()

        var allSuccessful = true
        var actionPerformed = false

        candidates.forEach { candidate ->
            val normalizedCandidate =
                candidate.normalized ?: normalizeForComparison(candidate.original)
            if (normalizedCandidate != null && existingNormalized.contains(normalizedCandidate)) {
                return@forEach
            }

            val result = contactsApi
                .blockedNumbers()
                .insert()
                .blockedNumbers(listOf(NewBlockedNumber(number = candidate.original)))
                .commit()

            if (result.isSuccessful) {
                actionPerformed = true
                normalizedCandidate?.let { existingNormalized.add(it) }
            } else {
                allSuccessful = false
            }
        }

        allSuccessful && (actionPerformed || candidates.all { candidate ->
            val normalizedCandidate =
                candidate.normalized ?: normalizeForComparison(candidate.original)
            normalizedCandidate != null && existingNormalized.contains(normalizedCandidate)
        })
    }

    suspend fun unblockContact(contact: Contact): Boolean = withContext(Dispatchers.IO) {
        val candidates = contact.blockCandidates()
        if (candidates.isEmpty()) {
            DiagnosticLog.warning(DiagnosticEvent.CONTACT_UNBLOCK_SKIPPED_NO_NUMBER)
            return@withContext false
        }

        val contactsApi = Contacts(context)
        val existing = contactsApi.queryBlockedNumbersIfSupported()

        fun registerKey(
            map: MutableMap<String, BlockedNumber>,
            key: String?,
            entry: BlockedNumber
        ) {
            if (!key.isNullOrBlank() && !map.containsKey(key)) {
                map[key] = entry
            }
        }

        val existingByKey = mutableMapOf<String, BlockedNumber>()
        existing.forEach { blocked ->
            val normalized = normalizeForComparison(blocked.normalizedNumber ?: blocked.number)
            val digits = digitsOnly(normalized) ?: digitsOnly(blocked.normalizedNumber)
            ?: digitsOnly(blocked.number)
            registerKey(existingByKey, normalized, blocked)
            registerKey(existingByKey, digits, blocked)
            digits?.let {
                if (it.length > 10) registerKey(existingByKey, it.takeLast(10), blocked)
                if (it.length > 7) registerKey(existingByKey, it.takeLast(7), blocked)
            }
        }

        fun candidateKeys(candidate: BlockCandidate): List<String> {
            val normalizedCandidate =
                candidate.normalized ?: normalizeForComparison(candidate.original)
            val digitsCandidate = digitsOnly(normalizedCandidate) ?: digitsOnly(candidate.original)
            val keys = mutableListOf<String>()
            normalizedCandidate?.let { keys.add(it) }
            digitsCandidate?.let { keys.add(it) }
            digitsCandidate?.let {
                if (it.length > 10) keys.add(it.takeLast(10))
                if (it.length > 7) keys.add(it.takeLast(7))
            }
            return keys
        }

        val toRemove = candidates.mapNotNull { candidate ->
            candidateKeys(candidate).firstNotNullOfOrNull { key -> existingByKey[key] }
        }.distinctBy { it.id }

        if (toRemove.isEmpty()) {
            DiagnosticLog.warning(DiagnosticEvent.CONTACT_UNBLOCK_MATCH_MISSING)
            return@withContext false
        }

        val deleteResult = contactsApi
            .blockedNumbers()
            .delete()
            .blockedNumbers(toRemove)
            .commit()

        deleteResult.isSuccessful
    }

    suspend fun deleteContact(contactIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        val contactsApi = Contacts(context)
        val lookupKeys = runCatching {
            contactsApi.query()
                .where { Contact.Id `in` contactIds }
                .find()
                .map { it.lookupKey }
        }.getOrElse { emptyList() }
        val deleteResult = contactsApi.delete()
            .contactsWithId(contactIds)
            .commit()
        if (deleteResult.isSuccessful) {
            lookupKeys.forEach { lookupKey ->
                contactPhotoStore.removeContactPhoto(lookupKey)
                incomingCallBackgroundStore.remove(lookupKey)
                inCallBackgroundStore.remove(lookupKey)
            }
        }
        deleteResult.isSuccessful
    }

    suspend fun insertContact(
        contact: Contact,
        account: ContactAccount,
        favorite: Boolean?,
        additionalEmails: List<String> = emptyList(),
    ): Boolean = withContext(Dispatchers.IO) {
        commitNewContact(Contacts(context), contact, account, favorite, additionalEmails).isSuccessful
    }

    suspend fun insertContactWithPhoto(
        contact: Contact,
        account: ContactAccount,
        favorite: Boolean?,
        additionalEmails: List<String>,
        photoUri: Uri?,
    ): ContactInsertResult = withContext(Dispatchers.IO) {
        val api = Contacts(context)
        val preparedPhoto = photoUri?.let { uri ->
            ImageProcessor.prepareContactPhoto(context, uri).getOrNull()
        }
        val result = commitNewContact(api, contact, account, favorite, additionalEmails)
        if (!result.isSuccessful) return@withContext ContactInsertResult.FAILED
        if (photoUri == null) return@withContext ContactInsertResult.INSERTED
        val inserted = result.contacts(api).firstOrNull()
        val photoSaved = preparedPhoto != null && inserted != null &&
            ContactPhotoWriter(context).write(inserted, api, preparedPhoto)
        if (photoSaved) ContactInsertResult.INSERTED else ContactInsertResult.INSERTED_WITHOUT_PHOTO
    }

    private fun commitNewContact(
        api: Contacts,
        contact: Contact,
        account: ContactAccount,
        favorite: Boolean?,
        additionalEmails: List<String>,
    ): Insert.Result = api.insert()
        .rawContact {
            setName {
                displayName = contact.name
            }

            account.toAccount()?.let(::setAccount)

            contact.phones.forEach { phone ->
                addPhone {
                    number = phone.number
                    type = phone.type
                }
            }

            (listOfNotNull(contact.email) + additionalEmails)
                .filter(String::isNotBlank)
                .distinct()
                .forEach { emailAddress ->
                    addEmail {
                        address = emailAddress
                        type = EmailEntity.Type.WORK
                    }
                }

            if (!contact.company.isNullOrBlank()) {
                setOrganization {
                    company = contact.company
                }
            }

            if (!contact.notes.isNullOrBlank()) {
                setNote {
                    note = contact.notes
                }
            }

            val contactBirthday = contact.birthday
            if (contactBirthday != null) {
                addEvent {
                    type = EventEntity.Type.BIRTHDAY
                    date = ContactUtils.longToEventDate(contactBirthday)
                }
            }

            contact.socialMedia.forEach { socialNetwork ->
                addWebsite {
                    url = socialNetwork
                }
            }

            setOptions {
                if (favorite != null) {
                    starred = favorite
                }
                customRingtone = contact.ringtone
            }
        }.commit()

    suspend fun getEmailAddresses(contactIds: List<Long>): Map<String, List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Contacts(context).query()
                    .where { Contact.Id `in` contactIds }
                    .find()
                    .associate { contact ->
                        contact.id.toString() to contact.emails()
                            .mapNotNull { email -> email.address?.takeIf(String::isNotBlank) }
                            .distinct()
                            .toList()
                    }
            }.getOrElse { emptyMap() }
        }

    suspend fun findExistingContactIds(contactIds: List<Long>): Set<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                Contacts(context).query()
                    .where { Contact.Id `in` contactIds }
                    .find()
                    .map { it.id }
                    .toSet()
            }.getOrElse { contactIds.toSet() }
        }

    suspend fun resolveWritableAccount(preferred: List<ContactAccount>): ContactAccount =
        withContext(Dispatchers.IO) {
            val candidates = preferred.filterNot { account ->
                account.isLocal || ContactAccount.ACCOUNTS_BLACKLIST.contains(account.name)
            }
            if (candidates.isEmpty()) return@withContext ContactAccount.LOCAL_ACCOUNT
            val available = runCatching {
                Contacts(context).accounts().query().find().map { it.name to it.type }.toSet()
            }.getOrElse { emptySet() }
            candidates.firstOrNull { (it.name to it.type) in available }
                ?: ContactAccount.LOCAL_ACCOUNT
        }

    suspend fun getIncomingCallBackgroundUri(contactId: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val lookupKey = findLookupKey(contactId) ?: return@withContext null
                incomingCallBackgroundStore.resolve(lookupKey)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }

    suspend fun updateIncomingCallBackground(
        contactId: String,
        change: IncomingCallBackgroundChange,
    ): IncomingCallBackgroundUpdateResult = withContext(Dispatchers.IO) {
        if (change == IncomingCallBackgroundChange.Unchanged) {
            return@withContext IncomingCallBackgroundUpdateResult.Success
        }
        val lookupKey = findLookupKey(contactId)
            ?: return@withContext IncomingCallBackgroundUpdateResult.ContactUnavailable
        try {
            when (change) {
                IncomingCallBackgroundChange.Unchanged ->
                    IncomingCallBackgroundUpdateResult.Success

                IncomingCallBackgroundChange.Remove -> {
                    if (incomingCallBackgroundStore.remove(lookupKey)) {
                        IncomingCallBackgroundUpdateResult.Success
                    } else {
                        IncomingCallBackgroundUpdateResult.StorageFailure
                    }
                }

                is IncomingCallBackgroundChange.Replace ->
                    incomingCallBackgroundStore.replace(lookupKey, change.sourceUri)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            IncomingCallBackgroundUpdateResult.StorageFailure
        }
    }

    suspend fun getInCallBackgroundUri(contactId: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val lookupKey = findLookupKey(contactId) ?: return@withContext null
                inCallBackgroundStore.resolve(lookupKey)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }

    suspend fun updateInCallBackground(
        contactId: String,
        change: InCallBackgroundChange,
    ): InCallBackgroundUpdateResult = withContext(Dispatchers.IO) {
        if (change == InCallBackgroundChange.Unchanged) {
            return@withContext InCallBackgroundUpdateResult.Success
        }
        val lookupKey = findLookupKey(contactId)
            ?: return@withContext InCallBackgroundUpdateResult.ContactUnavailable
        try {
            when (change) {
                InCallBackgroundChange.Unchanged ->
                    InCallBackgroundUpdateResult.Success

                InCallBackgroundChange.Remove -> {
                    if (inCallBackgroundStore.remove(lookupKey)) {
                        InCallBackgroundUpdateResult.Success
                    } else {
                        InCallBackgroundUpdateResult.StorageFailure
                    }
                }

                is InCallBackgroundChange.Replace ->
                    inCallBackgroundStore.replace(lookupKey, change.sourceUri)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            InCallBackgroundUpdateResult.StorageFailure
        }
    }

    private fun findLookupKey(contactId: String): String? {
        if (contactId.isBlank()) return null
        return Contacts(context).query()
            .where { Contact.Id equalTo contactId }
            .find()
            .firstOrNull()
            ?.lookupKey
    }

    suspend fun findContactByPhoneNumber(contacts: List<Contact>, phoneNumber: String): Contact? =
        withContext(Dispatchers.IO) {
            val trimmedNumber = phoneNumber.trim()
            if (trimmedNumber.isEmpty()) return@withContext null

            if (trimmedNumber.contains("*") || trimmedNumber.contains("#")) {
                val cleanedSearchNumber = trimmedNumber.replace(" ", "")
                return@withContext contacts.find { contact ->
                    contact.phones.any { phone ->
                        val cleanedContactNumber = phone.number.replace(" ", "")
                        cleanedContactNumber == cleanedSearchNumber
                    }
                }
            } else {
                val normalizedSearchNumber = normalizeForComparison(trimmedNumber)
                val digitsSearchNumber =
                    digitsOnly(normalizedSearchNumber) ?: digitsOnly(trimmedNumber)

                return@withContext contacts.find { contact ->
                    contact.phones.any { phone ->
                        val normalizedContactNumber = normalizeForComparison(phone.number)
                        val digitsContactNumber =
                            digitsOnly(normalizedContactNumber) ?: digitsOnly(phone.number)

                        val normalizedMatches = if (
                            normalizedSearchNumber != null && normalizedContactNumber != null
                        ) {
                            areNumbersEquivalent(
                                normalizedSearchNumber,
                                normalizedContactNumber
                            )
                        } else {
                            false
                        }

                        val digitsMatches = if (
                            digitsSearchNumber != null && digitsContactNumber != null
                        ) {
                            when {
                                digitsContactNumber == digitsSearchNumber -> true
                                digitsSearchNumber.length >= MIN_DIGIT_MATCH &&
                                        digitsContactNumber.length >= MIN_DIGIT_MATCH -> {
                                    digitsContactNumber.endsWith(digitsSearchNumber) ||
                                            digitsSearchNumber.endsWith(digitsContactNumber)
                                }

                                else -> false
                            }
                        } else {
                            false
                        }

                        normalizedMatches || digitsMatches
                    }
                }
            }
        }

    suspend fun getBlockedNumberInfos(contacts: List<Contact>): List<BlockedNumberInfo> =
        withContext(Dispatchers.IO) {
            val contactsApi = Contacts(context)
            val blockedEntries = contactsApi.queryBlockedNumbersIfSupported()
            if (blockedEntries.isEmpty()) return@withContext emptyList()

            val contactByNormalized = mutableMapOf<String, Contact>()
            val contactByDigits = mutableMapOf<String, Contact>()
            contacts.forEach { contact ->
                contact.phones.forEach { phone ->
                    val normalized = normalizeForComparison(phone.number)
                    normalized?.let { contactByNormalized.putIfAbsent(it, contact) }

                    val digitsOnly = digitsOnly(normalized) ?: digitsOnly(phone.number)
                    if (!digitsOnly.isNullOrEmpty()) {
                        contactByDigits.putIfAbsent(digitsOnly, contact)
                        if (digitsOnly.length > 10) {
                            contactByDigits.putIfAbsent(digitsOnly.takeLast(10), contact)
                        }
                        if (digitsOnly.length > 7) {
                            contactByDigits.putIfAbsent(digitsOnly.takeLast(7), contact)
                        }
                    }
                }
            }

            blockedEntries.map { entry ->
                val normalized = normalizeForComparison(entry.normalizedNumber ?: entry.number)
                val display = entry.number ?: entry.normalizedNumber ?: normalized ?: ""
                val digitsOnly = digitsOnly(normalized) ?: digitsOnly(entry.normalizedNumber)
                ?: digitsOnly(entry.number)
                val associatedContact = normalized?.let { contactByNormalized[it] }
                    ?: digitsOnly?.let { digits ->
                        contactByDigits[digits]
                            ?: if (digits.length > 10) contactByDigits[digits.takeLast(10)] else null
                    }
                val entryId = entry.id

                BlockedNumberInfo(
                    id = entryId,
                    displayNumber = display.ifBlank { normalized ?: "" },
                    normalizedNumber = normalized,
                    contact = associatedContact
                )
            }.sortedWith(
                compareBy(
                    { it.contact?.name ?: it.displayNumber },
                    { it.displayNumber }
                ))
        }

    suspend fun blockNumber(number: String): Boolean =
        withContext(Dispatchers.IO) {
            val contactsApi = Contacts(context)
            val alreadyBlocked = contactsApi.queryBlockedNumbersIfSupported().any { entry ->
                BlockedNumberMatchPolicy.isSameSubscriber(number, entry.number) ||
                    BlockedNumberMatchPolicy.isSameSubscriber(number, entry.normalizedNumber)
            }
            if (alreadyBlocked) return@withContext true

            contactsApi
                .blockedNumbers()
                .insert()
                .blockedNumbers(listOf(NewBlockedNumber(number = number)))
                .commit()
                .isSuccessful
        }

    suspend fun unblockNumber(number: String): Boolean =
        withContext(Dispatchers.IO) {
            val contactsApi = Contacts(context)
            val blockedEntries = contactsApi.queryBlockedNumbersIfSupported()
            if (blockedEntries.isEmpty()) return@withContext false

            val comparisonKeys = buildComparisonKeys(number)
            if (comparisonKeys.isEmpty()) return@withContext false

            val toRemove = blockedEntries.filter { entry ->
                val entryKeys = buildSet {
                    entry.number?.takeIf { it.isNotBlank() }?.let { add(it) }
                    entry.normalizedNumber?.takeIf { it.isNotBlank() }?.let { add(it) }
                    normalizeForComparison(entry.normalizedNumber ?: entry.number)?.let { add(it) }
                    val digits = digitsOnly(entry.normalizedNumber) ?: digitsOnly(entry.number)
                    digits?.let { add(it) }
                    digits?.let {
                        if (it.length > 10) add(it.takeLast(10))
                        if (it.length > 7) add(it.takeLast(7))
                    }
                }
                entryKeys.any { it in comparisonKeys }
            }

            if (toRemove.isEmpty()) return@withContext false

            contactsApi
                .blockedNumbers()
                .delete()
                .blockedNumbers(toRemove)
                .commit()
                .isSuccessful
        }

    private fun areNumbersEquivalent(first: String, second: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val defaultIso = resolveDefaultCountryIso()
            try {
                if (PhoneNumberUtils.areSamePhoneNumber(first, second, defaultIso)) {
                    return true
                }
            } catch (_: Throwable) {
                DiagnosticLog.warning(DiagnosticEvent.CONTACT_PHONE_COMPARE_FAILED)
            }
        } else {
            @Suppress("DEPRECATION")
            if (PhoneNumberUtils.compare(first, second)) return true
        }
        return first == second || first.trimStart('+') == second.trimStart('+')
    }

    private fun resolveDefaultCountryIso(): String {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simIso = telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
        val networkIso = telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
        val localeIso = context.resources.configuration.locales.takeIf { it.size() > 0 }
            ?.get(0)
            ?.country
            ?.takeIf { it.isNotBlank() }
        val fallbackIso = Locale.getDefault().country.takeIf { it.isNotBlank() }

        return listOfNotNull(simIso, networkIso, localeIso, fallbackIso)
            .firstOrNull()
            ?.uppercase(Locale.US)
            ?: Locale.US.country
    }

    private fun buildComparisonKeys(rawNumber: String?): Set<String> {
        val normalized = normalizeForComparison(rawNumber)
        return BlockedNumberMatchPolicy.keys(rawNumber, normalized)
    }
}

enum class ContactInsertResult {
    INSERTED,
    INSERTED_WITHOUT_PHOTO,
    FAILED,
}

data class BlockedNumberInfo(
    val id: Long,
    val displayNumber: String,
    val normalizedNumber: String?,
    val contact: Contact?
)

internal fun ContactsCoreContact.toAppContact(
    unknownName: String,
    resolvedPhotoUri: Uri? = photoUri,
): Contact {
    val idStr = id.toString()
    val name = displayNamePrimary ?: unknownName
    val accounts = rawContacts.map { ContactAccount.fromAccount(it.account) }
    val birthday = events().firstOrNull { it.type == EventEntity.Type.BIRTHDAY }?.date
    val phonesFiltered = getPhonesFiltered(phones().toList())
    val email = emails().firstOrNull()?.address
    val company = organizations().firstOrNull()?.company
    val notes = noteList().firstOrNull()?.note
    val isFavorite = options?.starred ?: false
    val ringtone = options?.customRingtone
    val contact = Contact(
        id = idStr,
        name = name,
        accounts = accounts,
        birthday = birthday?.let { ContactUtils.eventDateToLong(it) },
        phones = phonesFiltered,
        photoUri = resolvedPhotoUri,
        remotePhotoUri = null,
        email = email,
        company = company,
        notes = notes,
        socialMedia = websiteList().filter { it.url != null }.map { it.url!! },
        isFavorite = isFavorite,
        ringtone = ringtone
    )

    DiagnosticLog.debug(DiagnosticEvent.CONTACT_LOOKUP_COMPLETED)

    return contact
}

private fun getPhonesFiltered(phones: List<Phone>): List<ContactPhone> {
    val distinctPhones = phones
        .filter { it.number != null }
        .map { phone ->
            val cleanNumber = phone.number!!.replace(" ", "")
            val cleanNormalized = phone.normalizedNumber?.replace(" ", "")

            val numberWithoutCountryCode =
                if (cleanNumber.startsWith("+") && cleanNumber.length > 3) {
                    cleanNumber.substring(3)
                } else cleanNumber

            val normalizedWithoutCountryCode =
                if (cleanNormalized != null && cleanNormalized.startsWith("+") && cleanNormalized.length > 3) {
                    cleanNormalized.substring(3)
                } else cleanNormalized

            Triple(
                cleanNumber,
                numberWithoutCountryCode,
                normalizedWithoutCountryCode
            ) to phone.type
        }
        .distinctBy {
            setOf(it.first.second, it.first.third).filterNotNull()
        }

    return distinctPhones.map {
        ContactPhone(it.first.first, it.second)
    }
}

private fun isContactBlocked(contact: Contact, numbers: Set<String>): Boolean {
    if (numbers.isEmpty()) return false
    val contactPhones = contact.phones.mapNotNull { normalizeForComparison(it.number) }
    return contactPhones.any { numbers.contains(it) }
}

private data class BlockCandidate(
    val original: String,
    val normalized: String?
)

private fun Contact.blockCandidates(): List<BlockCandidate> {
    return phones.mapNotNull { phone ->
        val raw = phone.number.trim()
        if (raw.isEmpty()) return@mapNotNull null
        val sanitized = sanitizeNumberForBlocking(raw) ?: return@mapNotNull null
        val normalized = normalizeForComparison(raw)
        BlockCandidate(
            original = sanitized,
            normalized = normalized
        )
    }.distinctBy { it.normalized ?: it.original }
}

private fun sanitizeNumberForBlocking(number: String): String? {
    val cleaned = number.replace("[^0-9+]".toRegex(), "")
    return cleaned.takeIf { it.isNotBlank() }
}

private fun normalizeForComparison(number: String?): String? {
    if (number.isNullOrBlank()) return null
    return PhoneNumberUtils.normalizeNumber(number)?.takeIf { it.isNotBlank() }
}

private fun sanitizeNumberForMatching(number: String?): String? = normalizeForComparison(number)

private fun digitsOnly(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val digits = value.filter { it.isDigit() }
    return digits.takeIf { it.isNotBlank() }
}

private fun Contacts.queryBlockedNumbersIfSupported(): List<BlockedNumber> {
    return blockedNumbers().query().find()
}
