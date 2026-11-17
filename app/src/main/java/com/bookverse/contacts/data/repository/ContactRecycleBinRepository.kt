package com.bookverse.contacts.data.repository

import android.content.Context
import android.net.Uri
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.contacts.ContactRestoreResult
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.db.DeletedContactEntity
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.DeletedContact
import com.bookverse.contacts.domain.contacts.ContactRecycleBinPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ContactRecycleBinRepository(
    context: Context,
    private val contactRepository: ContactRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ContactRecycleBin {
    private val appContext = context.applicationContext
    private val dao by lazy { AppDatabase.getInstance(appContext).deletedContactDao() }
    private val photoDirectory by lazy { File(appContext.filesDir, PHOTO_DIRECTORY) }

    override fun observeDeletedContacts(): Flow<List<DeletedContact>> =
        dao.observeAll().map { entities ->
            entities.map { entity ->
                DeletedContact(
                    id = entity.id,
                    contact = entity.toContact(photoUri = photoFile(entity)?.let(Uri::fromFile)),
                    deletedAt = entity.deletedAt,
                )
            }
        }

    override suspend fun deleteToBin(contacts: List<Contact>): Boolean = withContext(Dispatchers.IO) {
        val deletable = contacts.filter { it.id.toLongOrNull() != null }
        if (deletable.isEmpty()) return@withContext false
        val contactIds = deletable.map { it.id.toLong() }
        val snapshotIds = mutableMapOf<Long, Long>()
        try {
            val deletedAt = clock()
            val emailsByContact = contactRepository.getEmailAddresses(contactIds)
            deletable.forEach { contact ->
                val additionalEmails = emailsByContact[contact.id].orEmpty()
                    .filterNot { it.equals(contact.email, ignoreCase = true) }
                val entity = DeletedContactEntity.from(
                    contact = contact,
                    additionalEmails = additionalEmails,
                    photoFileName = copyPhoto(contact.photoUri),
                    deletedAt = deletedAt,
                )
                snapshotIds[contact.id.toLong()] = try {
                    dao.insert(entity)
                } catch (error: Exception) {
                    entity.photoFileName?.let { File(photoDirectory, it).delete() }
                    throw error
                }
            }
        } catch (error: CancellationException) {
            withContext(NonCancellable) { discard(snapshotIds.values.toList()) }
            throw error
        } catch (_: Exception) {
            discard(snapshotIds.values.toList())
            return@withContext false
        }

        val deleted = contactRepository.deleteContact(contactIds)
        if (!deleted) {
            val stillPresent = contactRepository.findExistingContactIds(contactIds)
            discard(snapshotIds.filterKeys { it in stillPresent }.values.toList())
        }
        deleted
    }

    override suspend fun restore(id: Long): ContactRestoreResult = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: return@withContext ContactRestoreResult.FAILED
        val photo = photoFile(entity)
        val account = contactRepository.resolveWritableAccount(entity.accounts.orEmpty())
        val result = try {
            contactRepository.insertContactWithPhoto(
                contact = entity.toContact().copy(accounts = listOf(account)),
                account = account,
                favorite = entity.isFavorite,
                additionalEmails = entity.additionalEmails,
                photoUri = photo?.let(Uri::fromFile),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ContactInsertResult.FAILED
        }
        when (result) {
            ContactInsertResult.FAILED -> ContactRestoreResult.FAILED
            ContactInsertResult.INSERTED -> {
                discard(listOf(id))
                ContactRestoreResult.RESTORED
            }

            ContactInsertResult.INSERTED_WITHOUT_PHOTO -> {
                discard(listOf(id))
                ContactRestoreResult.RESTORED_WITHOUT_PHOTO
            }
        }
    }

    override suspend fun deleteForever(ids: List<Long>): Boolean = withContext(Dispatchers.IO) {
        try {
            discard(ids)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun purgeExpired(now: Long): Int = withContext(Dispatchers.IO) {
        try {
            val expired = dao.getAll()
                .filter { ContactRecycleBinPolicy.isExpired(it.deletedAt, now) }
                .map { it.id }
            discard(expired)
            expired.size
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun discard(ids: List<Long>) {
        if (ids.isEmpty()) return
        ids.chunked(QUERY_CHUNK_SIZE).forEach { chunk ->
            val entities = dao.getByIds(chunk)
            dao.deleteByIds(chunk)
            entities.forEach { entity -> photoFile(entity)?.delete() }
        }
    }

    private fun photoFile(entity: DeletedContactEntity): File? =
        entity.photoFileName?.let { name -> File(photoDirectory, name) }?.takeIf(File::exists)

    private fun copyPhoto(source: Uri?): String? {
        if (source == null) return null
        val fileName = "${UUID.randomUUID()}.jpg"
        val target = File(photoDirectory, fileName)
        return try {
            photoDirectory.mkdirs()
            val copied = appContext.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (copied != null && copied > 0L) {
                fileName
            } else {
                target.delete()
                null
            }
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    private companion object {
        const val PHOTO_DIRECTORY = "contact_recycle_bin"
        const val QUERY_CHUNK_SIZE = 500
    }
}
