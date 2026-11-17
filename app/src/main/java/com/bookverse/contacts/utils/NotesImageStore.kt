package com.bookverse.contacts.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.UUID

object NotesImageStore {
    const val DIRECTORY_NAME = "note_images"
    const val MAX_IMAGE_BYTES = 32L * 1024L * 1024L
    const val MAX_IMAGES_PER_PICK = 10

    private const val STORED_MAX_LONG_EDGE = 2048
    private const val STORED_JPEG_QUALITY = 90
    private const val ORPHAN_GRACE_MILLIS = 24L * 60L * 60L * 1000L
    private val fileNamePattern = Regex("^[A-Za-z0-9_-]{1,80}\\.(jpg|png)$")
    private val storedTarget = ImageTarget(
        maxLongEdge = STORED_MAX_LONG_EDGE,
        maxPixels = STORED_MAX_LONG_EDGE.toLong() * STORED_MAX_LONG_EDGE.toLong(),
    )

    fun isValidFileName(fileName: String): Boolean = fileNamePattern.matches(fileName)

    fun extensionOf(fileName: String): String? =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT).let { extension ->
            when (extension) {
                "jpg", "jpeg" -> "jpg"
                "png" -> "png"
                else -> null
            }
        }

    fun newFileName(extension: String): String =
        "${UUID.randomUUID().toString().replace("-", "")}.$extension"

    fun directory(context: Context): File =
        File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }

    fun fileFor(context: Context, fileName: String): File? =
        if (isValidFileName(fileName)) File(directory(context), fileName) else null

    suspend fun importFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        when (val decoded = ImageProcessor.decode(context, uri, storedTarget)) {
            ImageProcessingResult.Failure -> null
            is ImageProcessingResult.Success -> {
                val bitmap = decoded.bitmap
                try {
                    writeBitmap(context, bitmap)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }
    }

    suspend fun importFromStream(
        context: Context,
        input: InputStream,
        extension: String,
    ): String? = withContext(Dispatchers.IO) {
        val target = File(directory(context), newFileName(extension))
        try {
            target.outputStream().use { output ->
                input.copyBounded(output, MAX_IMAGE_BYTES)
            }
            target.name
        } catch (_: IOException) {
            target.delete()
            null
        }
    }

    suspend fun loadBitmap(
        context: Context,
        fileName: String,
        maxLongEdge: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(context, fileName)?.takeIf { it.isFile } ?: return@withContext null
        val edge = maxLongEdge.coerceAtLeast(1)
        val target = ImageTarget(maxLongEdge = edge, maxPixels = edge.toLong() * edge.toLong())
        when (val decoded = ImageProcessor.decode(context, Uri.fromFile(file), target)) {
            ImageProcessingResult.Failure -> null
            is ImageProcessingResult.Success -> decoded.bitmap
        }
    }

    suspend fun delete(context: Context, fileNames: Collection<String>) =
        withContext(Dispatchers.IO) {
            fileNames.forEach { fileName -> fileFor(context, fileName)?.delete() }
        }

    suspend fun deleteOrphans(
        context: Context,
        referencedFileNames: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        directory(context).listFiles().orEmpty().forEach { file ->
            if (
                file.isFile &&
                file.name !in referencedFileNames &&
                nowMillis - file.lastModified() > ORPHAN_GRACE_MILLIS
            ) {
                file.delete()
            }
        }
    }

    private fun writeBitmap(context: Context, bitmap: Bitmap): String? {
        val hasAlpha = bitmap.hasAlpha()
        val target = File(directory(context), newFileName(if (hasAlpha) "png" else "jpg"))
        return try {
            val written = target.outputStream().use { output ->
                if (hasAlpha) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, STORED_JPEG_QUALITY, output)
                }
            }
            if (written) {
                target.name
            } else {
                target.delete()
                null
            }
        } catch (_: IOException) {
            target.delete()
            null
        }
    }
}

internal fun InputStream.copyBounded(output: java.io.OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    var count = read(buffer)
    while (count >= 0) {
        total += count
        if (total > maxBytes) throw IOException("Entry exceeds the allowed size")
        output.write(buffer, 0, count)
        count = read(buffer)
    }
    return total
}
