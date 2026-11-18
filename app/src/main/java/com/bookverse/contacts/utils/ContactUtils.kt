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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.createBitmap
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.settings.LocaleController
import contacts.core.entities.EventDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ContactUtils {

    const val CONTACT_OBJECT = ContactShareFormat.CONTACT_OBJECT

    fun openUrl(context: Context, url: String): ExternalUriLauncher.Result = UrlUtils.openUrl(context, url)

    fun highlightText(text: String, query: String, highlightColor: Color): AnnotatedString {
        if (query.isEmpty()) return AnnotatedString(text)
        return buildAnnotatedString {
            val lowercaseText = text.lowercase()
            val lowercaseQuery = query.lowercase()
            var startIndex = 0
            while (startIndex < text.length) {
                val matchIndex = lowercaseText.indexOf(lowercaseQuery, startIndex)
                if (matchIndex == -1) {
                    append(text.substring(startIndex))
                    break
                }
                if (matchIndex > startIndex) {
                    append(text.substring(startIndex, matchIndex))
                }
                val endIndex = matchIndex + query.length
                withStyle(style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(matchIndex, endIndex))
                }
                startIndex = endIndex
            }
        }
    }

    fun getRandomColor(seed: String): Color {
        val colors = listOf(
            Color(0xFF1E88E5),
            Color(0xFF1565C0),
            Color(0xFF90CAF9),
            Color(0xFF43A047),
            Color(0xFF2E7D32),
            Color(0xFFA5D6A7),
            Color(0xFFD32F2F),
            Color(0xFFEF9A9A),
            Color(0xFFFB8C00),
            Color(0xFFEF6C00),
            Color(0xFFFFCC80),
            Color(0xFFFFA000),
            Color(0xFF7E57C2),
            Color(0xFFAF89F5),
            Color(0xFFC2185B),
            Color(0xFFF48FB1),
            Color(0xFF009688),
            Color(0xFF00695C),
            Color(0xFF80CBC4),
            Color(0xFF00ACC1),
            Color(0xFF80DEEA),
            Color(0xFF3949AB),
            Color(0xFF607D8B),
            Color(0xFFFBC02D)
        )
        val index = seed.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }
        return colors[index]
    }

    fun String.isValidEmail(): Boolean {
        return Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(this)
    }

    fun capitalizeFirstLetter(word: String): String {
        return word.first().uppercase() + word.substring(1)
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

    fun formatSeconds(context: Context, seconds: Int): String {
        if (seconds == 0) return ""
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m > 0 && s > 0 -> context.getString(
                when {
                    m == 1 && s == 1 -> R.string.call_duration_minute_second
                    m == 1 -> R.string.call_duration_minute_seconds
                    s == 1 -> R.string.call_duration_minutes_second
                    else -> R.string.call_duration_minutes_seconds
                },
                m,
                s,
            )

            m > 0 -> context.getString(
                if (m == 1) R.string.call_duration_minute else R.string.call_duration_minutes,
                m,
            )

            else -> context.getString(
                if (s == 1) R.string.call_duration_second else R.string.call_duration_seconds,
                s,
            )
        }
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

    fun getFirstName(fullName: String): String {
        val firstName = fullName.split(" ").first()
        return if (firstName.length > 2) firstName else fullName
    }

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

    fun String.toRelativeDate(
        context: Context,
        locale: Locale = LocaleController.effectiveLocale(context),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", locale)
        val parsedDate = try {
            LocalDate.parse(this, formatter)
        } catch (_: Exception) {
            return this
        }

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (parsedDate) {
            today -> context.getString(R.string.today)
            yesterday -> context.getString(R.string.yesterday)
            else -> this
        }
    }

    fun Long.toRelativeDate(
        context: Context,
        pattern: String = "MMM dd",
        locale: Locale = LocaleController.effectiveLocale(context),
    ): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        return when (date) {
            today -> context.getString(R.string.today)
            yesterday -> context.getString(R.string.yesterday)
            else -> date.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }

    fun contactPhotoMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)

    fun contactPhotoCropMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)
}
