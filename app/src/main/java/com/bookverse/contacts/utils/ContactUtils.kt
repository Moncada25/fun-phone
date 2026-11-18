package com.bookverse.contacts.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.SimDecision
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.domain.call.SimSelectionPolicy
import com.bookverse.contacts.service.PhoneAccountDirectory
import com.bookverse.contacts.service.SimSelectionCoordinator
import com.bookverse.contacts.settings.CallPreferences
import com.bookverse.contacts.settings.LocaleController
import contacts.core.entities.EventDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ContactUtils {

    const val CONTACT_OBJECT = "CONTACT_DATA:"

    private sealed interface PendingCallRequest {
        data class Number(val value: String) : PendingCallRequest
        data object Voicemail : PendingCallRequest
    }

    private var callPermissionLauncher: ActivityResultLauncher<String>? = null
    private var defaultDialerLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingCallRequest: PendingCallRequest? = null
    private val callScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private enum class DialType { NORMAL, USSD, SECRET }

    private fun classify(number: String): DialType {
        val n = number
        return when {
            n == "*#06#" -> DialType.SECRET
            n.contains('*') && n.endsWith('#') && n.any { it.isDigit() } -> DialType.USSD
            else -> DialType.NORMAL
        }
    }

    private fun buildDialUri(raw: String): Uri {
        val sanitized = raw.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        return Uri.fromParts("tel", sanitized, null)
    }

    fun setupCallPermissionLauncher(activity: ComponentActivity) {
        callPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted && pendingCallRequest != null) {
                resolvePendingCall(activity)
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.call_permission_required),
                    Toast.LENGTH_SHORT,
                ).show()
                pendingCallRequest = null
            }
        }

        defaultDialerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            PermissionUtils.handleDefaultDialerResult(
                context = activity,
                resultCode = result.resultCode,
                onSuccess = {
                    resolvePendingCall(activity)
                },
                onFailure = {
                    pendingCallRequest = null
                }
            )
        }
    }

    private fun resolvePendingCall(context: Context) {
        val request = pendingCallRequest ?: return
        pendingCallRequest = null
        when (request) {
            is PendingCallRequest.Number -> executeCall(context, request.value)
            PendingCallRequest.Voicemail -> executeVoicemailCall(context)
        }
    }

    fun callPhoneNumber(context: Context, phoneNumber: String) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_PHONE_REQUESTED)
        if (PermissionUtils.isDefaultDialer(context)) {
            executeCall(context, phoneNumber)
        } else {
            DiagnosticLog.debug(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUIRED)
            pendingCallRequest = PendingCallRequest.Number(phoneNumber)
            defaultDialerLauncher?.let { launcher ->
                PermissionUtils.requestSetAsDefaultDialer(
                    context = context,
                    launcher = launcher,
                    onFailure = {
                        DiagnosticLog.warning(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUEST_FAILED)
                        pendingCallRequest = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.call_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } ?: run {
                DiagnosticLog.error(DiagnosticEvent.CALL_DEFAULT_DIALER_LAUNCHER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.call_initialization_error),
                    Toast.LENGTH_SHORT,
                )
                    .show()
            }
        }
    }

    fun callVoicemail(context: Context) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_REQUESTED)
        if (PermissionUtils.isDefaultDialer(context)) {
            executeVoicemailCall(context)
        } else {
            DiagnosticLog.debug(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUIRED)
            pendingCallRequest = PendingCallRequest.Voicemail
            defaultDialerLauncher?.let { launcher ->
                PermissionUtils.requestSetAsDefaultDialer(
                    context = context,
                    launcher = launcher,
                    onFailure = {
                        DiagnosticLog.warning(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUEST_FAILED)
                        pendingCallRequest = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.voicemail_call_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } ?: run {
                DiagnosticLog.error(DiagnosticEvent.CALL_DEFAULT_DIALER_LAUNCHER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.voicemail_call_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun executeVoicemailCall(context: Context) {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val configuredNumber = try {
                telephonyManager?.voiceMailNumber
            } catch (_: SecurityException) {
                DiagnosticLog.warning(DiagnosticEvent.CALL_VOICEMAIL_FAILED)
                null
            }

            if (!configuredNumber.isNullOrBlank()) {
                DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_NUMBER_USED)
                executeCall(context, configuredNumber)
                return
            }

            val displayLabel = context.getString(R.string.voicemail)
            val voicemailUri = Uri.fromParts(PhoneAccount.SCHEME_VOICEMAIL, "", null)
            val telecomManager =
                context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            if (PermissionUtils.isDefaultDialer(context) && telecomManager != null) {
                try {
                    InCallNavigator.open(context, displayLabel)
                    telecomManager.placeCall(voicemailUri, Bundle())
                    DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_URI_USED)
                    return
                } catch (_: SecurityException) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                } catch (_: Exception) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                }
            }

            InCallNavigator.open(context, displayLabel)
            val intent = Intent(Intent.ACTION_CALL).apply { data = voicemailUri }
            addNewTaskIfNeeded(context, intent)
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(intent)
                DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_URI_USED)
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_VOICEMAIL_HANDLER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.voicemail_call_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_VOICEMAIL_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.voicemail_call_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addNewTaskIfNeeded(context: Context, intent: Intent) {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun isImeiSpecialCode(input: String): Boolean =
        input.trim().replace(" ", "") == "*#06#"

    private fun Context.openImeiInfoOrAboutPhone(): Boolean {
        val candidates = listOf(
            Intent("android.settings.IMEI_INFO_SETTINGS"),
            Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun executeCall(context: Context, phoneNumber: String) {
        try {
            DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)

            if (isImeiSpecialCode(phoneNumber)) {
                val opened = context.openImeiInfoOrAboutPhone()
                DiagnosticLog.debug(DiagnosticEvent.CALL_SPECIAL_CODE_REQUESTED)
                if (!opened) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.device_info_open_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            val accounts = PhoneAccountDirectory.callCapableAccounts(context)
            if (accounts.size < 2 || PhoneAccountDirectory.isEmergencyNumber(context, phoneNumber)) {
                placeCall(context, phoneNumber, null)
                return
            }

            callScope.launch {
                val preference = runCatching { CallPreferences.currentSimPreference(context) }
                    .getOrDefault(SimPreference.AskEveryTime)
                when (val decision = SimSelectionPolicy.decide(accounts, preference)) {
                    SimDecision.SystemDefault -> placeCall(context, phoneNumber, null)
                    is SimDecision.Use -> placeCall(context, phoneNumber, decision.accountId)
                    SimDecision.Ask -> {
                        val requested = SimSelectionCoordinator.requestForNewCall(phoneNumber, accounts)
                        if (!requested) placeCall(context, phoneNumber, null)
                    }
                }
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_EXECUTION_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.call_execution_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun callPhoneNumberWithSim(context: Context, phoneNumber: String, accountId: String) {
        placeCall(context, phoneNumber, accountId)
    }

    private fun placeCall(context: Context, phoneNumber: String, accountId: String?) {
        try {
            val type = classify(phoneNumber)
            val uri = buildDialUri(phoneNumber)
            val accountHandle = accountId?.let { PhoneAccountDirectory.handleFor(context, it) }
            DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)

            if (type == DialType.NORMAL) {
                InCallNavigator.open(context, phoneNumber)
            }

            if (PermissionUtils.isDefaultDialer(context)) {
                try {
                    val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val extras = Bundle().apply {
                        accountHandle?.let {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                        }
                    }
                    tm.placeCall(uri, extras)
                    DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)
                    return
                } catch (_: SecurityException) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                } catch (_: Exception) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                }
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = uri
                accountHandle?.let { putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
            }
            addNewTaskIfNeeded(context, intent)
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(intent)
                DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_HANDLER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.call_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_EXECUTION_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.call_execution_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String) {
        val formattedNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val url = "https://wa.me/$formattedNumber"
        openUrl(context, url)
    }

    fun openUrl(context: Context, url: String): ExternalUriLauncher.Result {
        val result = ExternalUriLauncher.open(context, url)
        if (result != ExternalUriLauncher.Result.OPENED) {
            Toast.makeText(
                context,
                context.getString(R.string.url_open_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
        return result
    }

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

    fun passwordStrengthInfo(strength: Int): Pair<Color, Int> {
        return when (strength) {
            0 -> Color(0xFFB71C1C) to R.string.strength_prohibited
            1 -> Color(0xFFD32F2F) to R.string.strength_very_weak
            2 -> Color(0xFFE65100) to R.string.strength_weak
            3 -> Color(0xFFF57C00) to R.string.strength_fair
            4 -> Color(0xFFF9A825) to R.string.strength_medium
            5 -> Color(0xFFFBC02D) to R.string.strength_medium
            6 -> Color(0xFFAFB42B) to R.string.strength_good
            7 -> Color(0xFF7CB342) to R.string.strength_strong
            8 -> Color(0xFF43A047) to R.string.strength_very_strong
            9 -> Color(0xFF2E7D32) to R.string.strength_excellent
            10 -> Color(0xFF1B5E20) to R.string.strength_excellent
            else -> Color(0xFF424242) to R.string.strength_weak
        }
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

    fun openSmsApp(context: Context, phoneNumber: String) {
        val uri = "smsto:$phoneNumber".toUri()
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        context.startActivity(intent)
    }

    fun contactPhotoMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)

    fun contactPhotoCropMaxDimension(context: Context): Int =
        ImageProcessor.contactCropMaxDimension(context)
}
