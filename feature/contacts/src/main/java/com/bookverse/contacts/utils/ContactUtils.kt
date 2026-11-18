package com.bookverse.contacts.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.settings.LocaleController
import contacts.core.entities.EventDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ContactUtils {

    const val CONTACT_OBJECT = ContactShareFormat.CONTACT_OBJECT

    fun openUrl(context: Context, url: String): ExternalUriLauncher.Result = UrlUtils.openUrl(context, url)

    fun String.isValidEmail(): Boolean {
        return Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(this)
    }

    fun longToEventDate(timestamp: Long): EventDate {
        val localDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return EventDate.from(
            localDate.year,
            localDate.monthValue - 1,
            localDate.dayOfMonth
        )
    }

    fun eventDateToLong(eventDate: EventDate?): Long? {
        if (eventDate == null) return null
        val year = eventDate.year ?: LocalDate.now().year
        val localDate = LocalDate.of(year, eventDate.month + 1, eventDate.dayOfMonth)
        return localDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun getLanguageCode(context: Context): String {
        return LocaleController.effectiveLocale(context).language.lowercase(Locale.ROOT)
    }

    fun copyToClipboard(
        context: Context,
        text: String,
        toast: Boolean = true,
        label: String? = null,
    ) = ClipboardUtils.copyToClipboard(context, text, toast, label)

    fun uriToBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val maxDimension = contactPhotoMaxDimension(context)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val target = ImageSizingPolicy.fit(
                    sourceWidth = info.size.width,
                    sourceHeight = info.size.height,
                    target = ImageTarget(
                        maxLongEdge = maxDimension,
                        maxPixels = maxDimension.toLong() * maxDimension.toLong(),
                    ),
                )
                decoder.setTargetSize(target.width, target.height)
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CONTACT_PHOTO_DECODE_FAILED)
            null
        }
    }

    fun Bitmap.asCircular(sizePx: Int? = null): Bitmap {
        val targetSize = sizePx ?: min(width, height)
        val out = createBitmap(targetSize, targetSize)

        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = max(targetSize.toFloat() / width, targetSize.toFloat() / height)
        val dx = (targetSize - width * scale) / 2f
        val dy = (targetSize - height * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        shader.setLocalMatrix(matrix)
        paint.shader = shader

        val r = targetSize / 2f
        canvas.drawCircle(r, r, r, paint)
        return out
    }

    fun contactPhotoMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)

    fun contactPhotoCropMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)
}
