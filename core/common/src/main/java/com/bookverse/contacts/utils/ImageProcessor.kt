package com.bookverse.contacts.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.graphics.scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ImageTarget(
    val maxLongEdge: Int,
    val maxPixels: Long,
)

data class ImageDimensions(
    val width: Int,
    val height: Int,
)

sealed interface ImageProcessingResult {
    data class Success(val bitmap: Bitmap) : ImageProcessingResult

    data object Failure : ImageProcessingResult
}

data class EncodedPhoto(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val format: ContactPhotoFormat,
)

data class PreparedContactPhoto(
    val localVariant: EncodedPhoto,
    val providerVariants: List<EncodedPhoto>,
)

enum class ContactPhotoFormat(val extension: String) {
    JPEG("jpg"),
    PNG("png"),
}

object ImageSizingPolicy {
    private const val PAINT_MAX_LONG_EDGE = 4096
    private const val PAINT_MAX_PIXELS = 8_000_000L
    private const val COLLAGE_TOTAL_PIXELS = 12_000_000L

    fun fit(
        sourceWidth: Int,
        sourceHeight: Int,
        target: ImageTarget,
    ): ImageDimensions {
        if (sourceWidth <= 0 || sourceHeight <= 0) return ImageDimensions(1, 1)

        val longEdgeScale = target.maxLongEdge.toDouble() /
            maxOf(sourceWidth, sourceHeight).toDouble()
        val pixelScale = sqrt(
            target.maxPixels.toDouble() /
                (sourceWidth.toLong() * sourceHeight.toLong()).toDouble(),
        )
        val scale = minOf(1.0, longEdgeScale, pixelScale)
        return ImageDimensions(
            width = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
            height = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
        )
    }

    fun paint(canvasWidth: Int, canvasHeight: Int): ImageTarget {
        val canvasLongEdge = maxOf(canvasWidth, canvasHeight).coerceAtLeast(1)
        return ImageTarget(
            maxLongEdge = minOf(PAINT_MAX_LONG_EDGE, canvasLongEdge * 2),
            maxPixels = PAINT_MAX_PIXELS,
        )
    }

    fun collage(
        requiredPhotos: Int,
        outputWidth: Int,
        outputHeight: Int,
    ): ImageTarget {
        val count = requiredPhotos.coerceAtLeast(1)
        val perPhotoPixels = COLLAGE_TOTAL_PIXELS / count
        return ImageTarget(
            maxLongEdge = minOf(
                PAINT_MAX_LONG_EDGE,
                maxOf(outputWidth, outputHeight).coerceAtLeast(1) * 2,
            ),
            maxPixels = perPhotoPixels,
        )
    }
}

object ContactPhotoSizingPolicy {
    const val LOCAL_MAX_DIMENSION = 2048
    const val LOCAL_JPEG_QUALITY = 97

    private const val MIN_PROVIDER_DIMENSION = 256
    private val providerScales = listOf(1f, 0.75f, 0.5f)

    fun cropMaxDimension(providerMaxDimension: Int): Int =
        maxOf(LOCAL_MAX_DIMENSION, providerMaxDimension.coerceAtLeast(1))

    fun localTarget(): ImageTarget = ImageTarget(
        maxLongEdge = LOCAL_MAX_DIMENSION,
        maxPixels = LOCAL_MAX_DIMENSION.toLong() * LOCAL_MAX_DIMENSION.toLong(),
    )

    fun providerTargets(providerMaxDimension: Int): List<ImageTarget> {
        val safeMaxDimension = providerMaxDimension.coerceAtLeast(1)
        return providerScales.map { scale ->
            val longEdge = (safeMaxDimension * scale)
                .roundToInt()
                .coerceAtLeast(MIN_PROVIDER_DIMENSION)
                .coerceAtMost(safeMaxDimension)
            ImageTarget(
                maxLongEdge = longEdge,
                maxPixels = longEdge.toLong() * longEdge.toLong(),
            )
        }.distinctBy(ImageTarget::maxLongEdge)
    }
}

object ImageProcessor {
    private const val FALLBACK_CONTACT_DIMENSION = 2048
    private val decodeScales = listOf(1f, 0.7f, 0.5f)
    private val jpegQualities = listOf(98, 95, 92)

    fun contactDisplayMaxDimension(context: Context): Int {
        return runCatching {
            context.contentResolver.query(
                ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                arrayOf(ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM)
                if (index < 0) null else cursor.getInt(index).takeIf { it > 0 }
            }
        }.getOrNull() ?: FALLBACK_CONTACT_DIMENSION
    }

    fun contactCropMaxDimension(context: Context): Int =
        ContactPhotoSizingPolicy.cropMaxDimension(contactDisplayMaxDimension(context))

    suspend fun decode(
        context: Context,
        uri: Uri,
        target: ImageTarget,
    ): ImageProcessingResult = withContext(Dispatchers.IO) {
        decodeScales.forEach { scale ->
            coroutineContext.ensureActive()
            val scaledTarget = target.copy(
                maxLongEdge = (target.maxLongEdge * scale).roundToInt().coerceAtLeast(1),
                maxPixels = (target.maxPixels * scale * scale).toLong().coerceAtLeast(1L),
            )
            val bitmap = decodeOnce(context, uri, scaledTarget)
            if (bitmap != null) return@withContext ImageProcessingResult.Success(bitmap)
        }
        ImageProcessingResult.Failure
    }

    suspend fun prepareContactPhoto(
        context: Context,
        uri: Uri,
    ): Result<PreparedContactPhoto> = withContext(Dispatchers.IO) {
        val providerMaxDimension = contactDisplayMaxDimension(context)
        val decodeMaxDimension = ContactPhotoSizingPolicy.cropMaxDimension(providerMaxDimension)
        when (
            val decoded = decode(
                context = context,
                uri = uri,
                target = ImageTarget(
                    maxLongEdge = decodeMaxDimension,
                    maxPixels = decodeMaxDimension.toLong() * decodeMaxDimension.toLong(),
                ),
            )
        ) {
            ImageProcessingResult.Failure ->
                Result.failure(IllegalArgumentException("Unable to decode contact photo"))

            is ImageProcessingResult.Success -> {
                val source = decoded.bitmap
                try {
                    val format = if (containsTransparency(source)) {
                        ContactPhotoFormat.PNG
                    } else {
                        ContactPhotoFormat.JPEG
                    }
                    val localVariant = createEncodedVariant(
                        source = source,
                        target = ContactPhotoSizingPolicy.localTarget(),
                        format = format,
                        jpegQuality = ContactPhotoSizingPolicy.LOCAL_JPEG_QUALITY,
                    )
                    val providerVariants = ContactPhotoSizingPolicy
                        .providerTargets(providerMaxDimension)
                        .mapIndexed { index, target ->
                            coroutineContext.ensureActive()
                            createEncodedVariant(
                                source = source,
                                target = target,
                                format = format,
                                jpegQuality = jpegQualities[index],
                            )
                        }.distinctBy { it.width to it.height }
                    Result.success(
                        PreparedContactPhoto(
                            localVariant = localVariant,
                            providerVariants = providerVariants,
                        ),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Result.failure(error)
                } finally {
                    if (!source.isRecycled) source.recycle()
                }
            }
        }
    }

    private fun decodeOnce(
        context: Context,
        uri: Uri,
        target: ImageTarget,
    ): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val dimensions = ImageSizingPolicy.fit(
                    sourceWidth = info.size.width,
                    sourceHeight = info.size.height,
                    target = target,
                )
                if (
                    dimensions.width != info.size.width ||
                    dimensions.height != info.size.height
                ) {
                    decoder.setTargetSize(dimensions.width, dimensions.height)
                }
            }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun createEncodedVariant(
        source: Bitmap,
        target: ImageTarget,
        format: ContactPhotoFormat,
        jpegQuality: Int,
    ): EncodedPhoto {
        val dimensions = ImageSizingPolicy.fit(
            sourceWidth = source.width,
            sourceHeight = source.height,
            target = target,
        )
        val variant = if (
            dimensions.width == source.width &&
            dimensions.height == source.height
        ) {
            source
        } else {
            source.scale(dimensions.width, dimensions.height, filter = true)
        }
        return try {
            EncodedPhoto(
                bytes = encodeBitmap(
                    bitmap = variant,
                    format = format,
                    jpegQuality = jpegQuality,
                ),
                width = variant.width,
                height = variant.height,
                format = format,
            )
        } finally {
            if (variant !== source && !variant.isRecycled) variant.recycle()
        }
    }

    private fun containsTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val row = IntArray(bitmap.width)
        repeat(bitmap.height) { y ->
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { pixel -> pixel ushr 24 != 0xFF }) return true
        }
        return false
    }

    private fun encodeBitmap(
        bitmap: Bitmap,
        format: ContactPhotoFormat,
        jpegQuality: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val bitmapFormat = when (format) {
            ContactPhotoFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ContactPhotoFormat.PNG -> Bitmap.CompressFormat.PNG
        }
        val quality = if (format == ContactPhotoFormat.PNG) 100 else jpegQuality
        check(bitmap.compress(bitmapFormat, quality, output)) {
            "Unable to encode contact photo"
        }
        return output.toByteArray()
    }
}
