package com.bookverse.contacts.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.domain.passwords.PasswordImportPolicy
import com.bookverse.contacts.domain.passwords.Totp
import com.bookverse.contacts.domain.passwords.PasswordImportResult
import com.bookverse.contacts.security.CryptoManager
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PasswordJsonUtils {
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()

    fun passwordCredentialToMap(c: PasswordCredential): Map<String, Any?> {
        return mapOf(
            "name" to c.name,
            "username" to c.username,
            "password" to c.password,
            "note" to c.note,
            "updatedAt" to c.updatedAt,
            "createdAt" to c.createdAt,
            "url" to c.url,
        )
    }

    fun buildPasswordsBackupJson(
        credentials: List<PasswordCredential>,
        folders: List<PasswordFolder>,
    ): String {
        val payload = mapOf(
            "type" to "passwords",
            "version" to 2,
            "folders" to folders.map { folder ->
                mapOf(
                    "id" to folder.id,
                    "name" to folder.name,
                    "description" to folder.description,
                    "colorHex" to folder.colorHex,
                    "createdAt" to folder.createdAt,
                )
            },
            "credentials" to credentials.filterNot { it.isDeleted }.map { credential ->
                passwordCredentialToMap(credential) +
                    ("folderId" to credential.folderId) +
                    ("totp" to credential.totp)
            },
        )
        return gson.toJson(payload)
    }

    suspend fun sharePasswordCredential(context: Context, credential: PasswordCredential) =
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(passwordCredentialToMap(credential))
                val file = File(context.cacheDir, "password_${UUID.randomUUID()}.json")
                FileOutputStream(file).use { it.write(json.toByteArray()) }
                val uri =
                    FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.password_share_chooser)
                        )
                    )
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.passwords_json_saved_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    suspend fun exportPasswords(
        context: Context,
        credentials: List<PasswordCredential>,
        folders: List<PasswordFolder>,
        uri: Uri,
    ) =
        withContext(Dispatchers.IO) {
            try {
                val json = buildPasswordsBackupJson(credentials, folders)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.passwords_json_saved_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.passwords_json_saved_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    /** Compatibility overload for callers that only have the legacy credential list. */
    suspend fun exportPasswords(
        context: Context,
        credentials: List<PasswordCredential>,
        uri: Uri,
    ) = exportPasswords(context, credentials, emptyList(), uri)

    suspend fun importPasswords(
        context: Context,
        uri: Uri,
        viewModel: PasswordsViewModel,
    ): PasswordImportResult =
        withContext(Dispatchers.IO) {
            try {
                val json =
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: return@withContext PasswordImportResult(imported = 0, skipped = 0)
                val root = JSONTokener(json).nextValue()
                val credentialsArray = when (root) {
                    is JSONArray -> root
                    is JSONObject -> root.optJSONArray("credentials")
                    else -> null
                } ?: throw IllegalArgumentException("Missing credentials")

                val folderMapping = mutableMapOf<Long, Long>()
                if (root is JSONObject) {
                    val existingFolders = viewModel.getFoldersForBackup().toMutableList()
                    val foldersArray = root.optJSONArray("folders") ?: JSONArray()
                    for (index in 0 until foldersArray.length()) {
                        val item = foldersArray.optJSONObject(index) ?: continue
                        val oldId = item.optLong("id", -1L)
                        val name = item.optString("name").trim()
                        if (oldId < 0 || name.isBlank()) continue
                        val existing = existingFolders.firstOrNull {
                            it.name.equals(name, ignoreCase = true)
                        }
                        val newId = existing?.id ?: viewModel.importFolder(
                            PasswordFolder(
                                name = name,
                                description = item.optNullableString("description"),
                                colorHex = item.optString("colorHex", "#90CAF9"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            ),
                        ).also { id ->
                            existingFolders += PasswordFolder(
                                id = id,
                                name = name,
                                description = item.optNullableString("description"),
                                colorHex = item.optString("colorHex", "#90CAF9"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            )
                        }
                        folderMapping[oldId] = newId
                    }
                }

                val candidates = mutableListOf<PasswordCredential>()
                for (index in 0 until credentialsArray.length()) {
                    val item = credentialsArray.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    val username = item.optString("username")
                    val storedPassword = item.optString("password")

                    val password = if (storedPassword.isNotEmpty()) {
                        try {
                            val encryptedBytes = Base64.decode(storedPassword, Base64.NO_WRAP)
                            val decryptedBytes = CryptoManager.decrypt(context, encryptedBytes)
                            decryptedBytes?.let { String(it, Charsets.UTF_8) } ?: storedPassword
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            storedPassword
                        }
                    } else {
                        ""
                    }

                    if (name.isBlank()) continue
                    val notes = item.optNullableString("note")
                    val updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                    val createdAt = item.optLong("createdAt", updatedAt)
                    val oldFolderId = if (item.has("folderId") && !item.isNull("folderId")) {
                        item.optLong("folderId")
                    } else {
                        null
                    }
                    candidates += PasswordCredential(
                        id = 0,
                        name = name,
                        username = username,
                        password = password,
                        note = notes,
                        updatedAt = updatedAt,
                        createdAt = createdAt,
                        folderId = oldFolderId?.let { folderMapping[it] },
                        url = item.optNullableString("url"),
                        totp = item.optNullableString("totp")
                            ?.let(Totp::parse)
                            ?.let(Totp::toUri),
                    )
                }
                val plan = PasswordImportPolicy.plan(viewModel.getAllForBackup(), candidates)
                plan.toImport.forEach { viewModel.importCredential(it) }
                PasswordImportResult(imported = plan.toImport.size, skipped = plan.skipped)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.passwords_json_import_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
                PasswordImportResult(imported = 0, skipped = 0)
            }
        }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotEmpty() }
    }
}
