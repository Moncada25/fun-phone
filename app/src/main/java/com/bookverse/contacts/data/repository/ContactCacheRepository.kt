package com.bookverse.contacts.data.repository

import android.content.Context
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.db.CachedContactEntity
import com.bookverse.contacts.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ContactCacheRepository(context: Context) {

    private val database by lazy { AppDatabase.getInstance(context) }
    private val cachedContactDao get() = database.cachedContactDao()

    suspend fun getCachedContacts(): List<Contact> = withContext(Dispatchers.IO) {
        cachedContactDao.getContacts().map { it.toContact() }
    }

    suspend fun getCachedProfile(): Contact? = withContext(Dispatchers.IO) {
        cachedContactDao.getProfile()?.toContact()
    }

    fun observeCachedContacts(): Flow<List<Contact>> =
        cachedContactDao.observeContacts().map { entities ->
            entities.map { it.toContact() }
        }

    fun observeCachedProfile(): Flow<Contact?> =
        cachedContactDao.observeProfile().map { entity -> entity?.toContact() }

    suspend fun replaceCachedContacts(contacts: List<Contact>, profile: Contact?) =
        withContext(Dispatchers.IO) {
            val contactEntities = contacts.map { CachedContactEntity.from(it, isProfile = false) }
            val profileEntity = profile?.let { CachedContactEntity.from(it, isProfile = true) }
            cachedContactDao.replaceAll(contactEntities, profileEntity)
        }
}
