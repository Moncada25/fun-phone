package com.bookverse.contacts.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.bookverse.contacts.utils.EncodedPhoto
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal class ContactPhotoStore(context: Context) {
    private val directory = File(context.filesDir, DIRECTORY_NAME)

    fun resolveContactPhoto(lookupKey: String?, providerUri: Uri?): Uri? =
        lookupKey?.takeIf { it.isNotBlank() }
            ?.let { resolve(contactBaseName(it)) }
            ?: providerUri

    fun resolveProfilePhoto(providerUri: Uri?): Uri? =
        resolve(PROFILE_BASE_NAME) ?: providerUri

    fun saveContactPhoto(lookupKey: String?, photo: EncodedPhoto): Boolean =
        !lookupKey.isNullOrBlank() && save(contactBaseName(lookupKey), photo)

    fun saveProfilePhoto(photo: EncodedPhoto): Boolean = save(PROFILE_BASE_NAME, photo)

    fun removeContactPhoto(lookupKey: String?): Boolean =
        lookupKey.isNullOrBlank() || remove(contactBaseName(lookupKey))

    fun removeProfilePhoto(): Boolean = remove(PROFILE_BASE_NAME)

    fun reconcileContacts(activeLookupKeys: Set<String?>) {
        if (!directory.exists()) return
        val activeBaseNames = activeLookupKeys
            .asSequence()
            .filterNotNull()
            .filter { it.isNotBlank() }
            .map(::contactBaseName)
            .toSet()
        directory.listFiles()?.forEach { file ->
            when {
                file.name.endsWith(TEMP_SUFFIX) -> file.delete()
                file.name.startsWith(CONTACT_PREFIX) &&
                    file.name.substringBeforeLast('.') !in activeBaseNames -> file.delete()
            }
        }
    }

    private fun resolve(baseName: String): Uri? {
        val file = filesFor(baseName)
            .filter(File::isFile)
            .maxByOrNull(File::lastModified)
            ?: return null
        if (!isDecodableImage(file)) {
            remove(baseName)
            return null
        }
        val version = "${file.lastModified()}-${file.length()}"
        return file.toUri().buildUpon()
            .appendQueryParameter(VERSION_QUERY, version)
            .build()
    }

    private fun save(baseName: String, photo: EncodedPhoto): Boolean {
        if (photo.bytes.isEmpty() || photo.width <= 0 || photo.height <= 0) return false
        return runCatching {
            check(directory.exists() || directory.mkdirs())
            val previousTimestamp = filesFor(baseName).maxOfOrNull(File::lastModified) ?: 0L
            val destination = File(directory, "$baseName.${photo.format.extension}")
            val temporary = File(
                directory,
                ".$baseName-${UUID.randomUUID()}$TEMP_SUFFIX",
            )
            try {
                FileOutputStream(temporary).use { stream ->
                    stream.write(photo.bytes)
                    stream.flush()
                    stream.fd.sync()
                }
                moveReplacing(temporary, destination)
                val versionTimestamp = maxOf(System.currentTimeMillis(), previousTimestamp + 1L)
                destination.setLastModified(versionTimestamp)
                filesFor(baseName)
                    .filter { it != destination }
                    .forEach(File::delete)
                true
            } finally {
                temporary.delete()
            }
        }.getOrDefault(false)
    }

    private fun remove(baseName: String): Boolean {
        val files = filesFor(baseName)
        return files.all { file -> !file.exists() || file.delete() }
    }

    private fun filesFor(baseName: String): List<File> =
        SUPPORTED_EXTENSIONS.map { extension -> File(directory, "$baseName.$extension") }

    private fun isDecodableImage(file: File): Boolean {
        if (file.length() <= 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun moveReplacing(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun contactBaseName(lookupKey: String): String =
        CONTACT_PREFIX + MessageDigest.getInstance("SHA-256")
            .digest(lookupKey.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val DIRECTORY_NAME = "contact_photos"
        const val CONTACT_PREFIX = "contact_"
        const val PROFILE_BASE_NAME = "profile"
        const val TEMP_SUFFIX = ".tmp"
        const val VERSION_QUERY = "v"
        val SUPPORTED_EXTENSIONS = listOf("jpg", "png")
    }
}
