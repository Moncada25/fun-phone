package com.bookverse.contacts.data.password

import android.content.Context
import com.bookverse.contacts.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.Normalizer
import kotlin.time.Duration.Companion.milliseconds

data class PasswordCredential(
    val id: Long = 0,
    val name: String,
    val username: String,
    val password: String,
    val note: String?,
    val updatedAt: Long,
    val createdAt: Long,
    val folderId: Long? = null,
    val isDeleted: Boolean = false,
    val url: String? = null,
    val totp: String? = null,
)

class PasswordRepository(
    private val dao: PasswordDao,
    private val context: Context
) : PasswordStore {
    private val searchQuery = MutableStateFlow("")

    private fun normalize(s: String?): String {
        if (s == null) return ""
        val n = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return n.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun decryptEntity(e: PasswordEntity): PasswordCredential {
        val username = CryptoManager.decrypt(context, e.usernameEnc)?.toString(Charsets.UTF_8)
        val password = CryptoManager.decrypt(context, e.passwordEnc)?.toString(Charsets.UTF_8)
        val notes = CryptoManager.decrypt(context, e.notesEnc)?.toString(Charsets.UTF_8)
        return PasswordCredential(
            id = e.id,
            name = e.name!!,
            username = username!!,
            password = password!!,
            note = notes,
            updatedAt = e.updatedAt,
            createdAt = e.createdAt,
            folderId = e.folderId,
            isDeleted = e.isDeleted,
            url = CryptoManager.decrypt(context, e.urlEnc)?.toString(Charsets.UTF_8),
            totp = CryptoManager.decrypt(context, e.totpEnc)?.toString(Charsets.UTF_8),
        )
    }

    private val decrypted: Flow<List<PasswordCredential>> =
        dao.observeAll().map { list -> list.map { decryptEntity(it) } }.flowOn(Dispatchers.IO)

    override val allCredentials: Flow<List<PasswordCredential>> get() = decrypted

    override val deletedCredentials: Flow<List<PasswordCredential>> =
        dao.observeDeleted().map { list -> list.map { decryptEntity(it) } }.flowOn(Dispatchers.IO)

    override val folders: Flow<List<PasswordFolder>> = dao.observeFolders()

    @OptIn(FlowPreview::class)
    override val credentials: Flow<List<PasswordCredential>> =
        combine(decrypted, searchQuery.debounce(150.milliseconds)) { list, q ->
            val normQ = normalize(q)
            if (normQ.isEmpty()) list else list.filter { c ->
                val fields = listOfNotNull(c.name, c.username, c.note, c.url)
                fields.any { normalize(it).contains(normQ) }
            }
        }

    override suspend fun get(id: Long): PasswordCredential? = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { decryptEntity(it) }
    }

    override suspend fun getAllForBackup(): List<PasswordCredential> = withContext(Dispatchers.IO) {
        dao.getAllSync().map { decryptEntity(it) }
    }

    override suspend fun getFoldersForBackup(): List<PasswordFolder> = withContext(Dispatchers.IO) {
        dao.getFoldersSync()
    }

    override fun setSearch(query: String) {
        searchQuery.value = query
    }

    override suspend fun upsert(credential: PasswordCredential) {
        withContext(Dispatchers.IO) {
            val entity = PasswordEntity(
                id = credential.id,
                name = credential.name,
                usernameEnc = CryptoManager.encrypt(context, credential.username.toByteArray()),
                passwordEnc = CryptoManager.encrypt(context, credential.password.toByteArray()),
                notesEnc = CryptoManager.encrypt(context, credential.note?.toByteArray()),
                updatedAt = System.currentTimeMillis(),
                createdAt = if (credential.id == 0L) {
                    System.currentTimeMillis()
                } else {
                    credential.createdAt
                },
                folderId = credential.folderId,
                isDeleted = credential.isDeleted,
                urlEnc = CryptoManager.encrypt(
                    context,
                    credential.url?.takeIf { it.isNotBlank() }?.toByteArray(),
                ),
                totpEnc = CryptoManager.encrypt(
                    context,
                    credential.totp?.takeIf { it.isNotBlank() }?.toByteArray(),
                ),
            )
            if (credential.id == 0L) {
                dao.insert(entity)
            } else {
                dao.update(entity)
            }
        }
    }

    override suspend fun markAsDeleted(id: Long) = withContext(Dispatchers.IO) {
        dao.markAsDeleted(id)
    }

    override suspend fun restore(id: Long) = withContext(Dispatchers.IO) {
        dao.restore(id)
    }

    override suspend fun permanentlyDelete(id: Long) {
        withContext(Dispatchers.IO) {
            dao.getById(id)?.let { dao.delete(it) }
        }
    }

    override suspend fun insertFolder(folder: PasswordFolder): Long = withContext(Dispatchers.IO) {
        dao.insertFolder(folder)
    }

    override suspend fun updateFolder(folder: PasswordFolder) = withContext(Dispatchers.IO) {
        dao.updateFolder(folder)
    }

    override suspend fun deleteFolder(folder: PasswordFolder) = withContext(Dispatchers.IO) {
        dao.deleteFolder(folder)
    }

    override suspend fun clearFolderAssignments(folderId: Long) = withContext(Dispatchers.IO) {
        dao.clearFolderAssignments(folderId)
    }

    override suspend fun moveToFolder(id: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        dao.moveToFolder(id, folderId)
    }
}
