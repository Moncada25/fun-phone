package com.bookverse.contacts.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext

sealed interface IncomingCallBackgroundChange {
    data object Unchanged : IncomingCallBackgroundChange

    data object Remove : IncomingCallBackgroundChange

    data class Replace(val sourceUri: Uri) : IncomingCallBackgroundChange
}

sealed interface IncomingCallBackgroundUpdateResult {
    data object Success : IncomingCallBackgroundUpdateResult

    data object ContactUnavailable : IncomingCallBackgroundUpdateResult

    data object ImageTooLarge : IncomingCallBackgroundUpdateResult

    data object InvalidImage : IncomingCallBackgroundUpdateResult

    data object StorageFailure : IncomingCallBackgroundUpdateResult
}

internal class IncomingCallBackgroundStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DIRECTORY_NAME)

    fun resolve(lookupKey: String?): Uri? {
        if (lookupKey.isNullOrBlank()) return null
        val file = fileFor(lookupKey)
        if (!file.isFile) return null
        if (!isDecodableImage(file)) {
            file.delete()
            return null
        }
        val version = "${file.lastModified()}-${file.length()}"
        return file.toUri().buildUpon()
            .appendQueryParameter(VERSION_QUERY, version)
            .build()
    }

    suspend fun replace(
        lookupKey: String?,
        sourceUri: Uri,
    ): IncomingCallBackgroundUpdateResult {
        if (lookupKey.isNullOrBlank()) {
            return IncomingCallBackgroundUpdateResult.ContactUnavailable
        }
        val temporary = File(
            directory,
            ".${fileBaseName(lookupKey)}-${UUID.randomUUID()}$TEMP_SUFFIX",
        )
        return try {
            check(directory.exists() || directory.mkdirs())
            val destination = fileFor(lookupKey)
            val previousTimestamp = destination.lastModified()
            val copyResult = copySource(sourceUri, temporary)
            if (copyResult != IncomingCallBackgroundUpdateResult.Success) {
                return copyResult
            }
            if (!isDecodableImage(temporary)) {
                return IncomingCallBackgroundUpdateResult.InvalidImage
            }
            moveReplacing(temporary, destination)
            destination.setLastModified(maxOf(System.currentTimeMillis(), previousTimestamp + 1L))
            IncomingCallBackgroundUpdateResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            IncomingCallBackgroundUpdateResult.StorageFailure
        } finally {
            temporary.delete()
        }
    }

    fun remove(lookupKey: String?): Boolean {
        if (lookupKey.isNullOrBlank()) return true
        val file = fileFor(lookupKey)
        return !file.exists() || file.delete()
    }

    fun reconcile(activeLookupKeys: Set<String?>) {
        if (!directory.exists()) return
        // Contact refresh can overlap replace(); fresh temporary files may still be copying.
        val staleTemporaryCutoff = System.currentTimeMillis() - STALE_TEMPORARY_FILE_AGE_MILLIS
        val activeNames = activeLookupKeys
            .asSequence()
            .filterNotNull()
            .filter(String::isNotBlank)
            .map(::fileName)
            .toSet()
        directory.listFiles()?.forEach { file ->
            when {
                file.name.endsWith(TEMP_SUFFIX) && file.lastModified() < staleTemporaryCutoff -> {
                    file.delete()
                }
                file.name.startsWith(FILE_PREFIX) && file.name !in activeNames -> file.delete()
            }
        }
    }

    private suspend fun copySource(
        sourceUri: Uri,
        destination: File,
    ): IncomingCallBackgroundUpdateResult {
        val input = if (sourceUri.scheme == "file") {
            sourceUri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
        } else {
            appContext.contentResolver.openInputStream(sourceUri)
        } ?: return IncomingCallBackgroundUpdateResult.InvalidImage

        input.use { source ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var totalBytes = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer)
                    if (read < 0) break
                    totalBytes += read
                    if (totalBytes > MAX_SOURCE_BYTES) {
                        return IncomingCallBackgroundUpdateResult.ImageTooLarge
                    }
                    output.write(buffer, 0, read)
                }
                if (totalBytes == 0L) {
                    return IncomingCallBackgroundUpdateResult.InvalidImage
                }
                output.flush()
                output.fd.sync()
            }
        }
        return IncomingCallBackgroundUpdateResult.Success
    }

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

    private fun fileFor(lookupKey: String): File = File(directory, fileName(lookupKey))

    private fun fileName(lookupKey: String): String = "${fileBaseName(lookupKey)}$FILE_EXTENSION"

    private fun fileBaseName(lookupKey: String): String = FILE_PREFIX +
        MessageDigest.getInstance("SHA-256")
            .digest(lookupKey.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        internal const val MAX_SOURCE_BYTES = 50L * 1024L * 1024L
        private const val COPY_BUFFER_SIZE = 32 * 1024
        private const val DIRECTORY_NAME = "incoming_call_backgrounds"
        private const val FILE_PREFIX = "background_"
        private const val FILE_EXTENSION = ".img"
        private const val TEMP_SUFFIX = ".tmp"
        private const val VERSION_QUERY = "v"
        internal const val STALE_TEMPORARY_FILE_AGE_MILLIS = 60L * 60L * 1000L
    }
}
