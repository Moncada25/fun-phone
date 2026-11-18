package com.bookverse.contacts.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

object ExternalUriLauncher {
    enum class Result {
        OPENED,
        NO_HANDLER,
        INVALID_URI,
        FAILED,
    }

    internal data class PreparedIntent(
        val intent: Intent,
        val browserFallbackUrl: String? = null,
    )

    private const val BROWSER_FALLBACK_URL = "browser_fallback_url"
    private val blockedSchemes = setOf(
        "about",
        "content",
        "data",
        "file",
        "javascript",
        "intent",
    )
    private val packageNamePattern = Regex(
        "^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$",
    )

    fun open(context: Context, rawValue: String): Result {
        val prepared = runCatching { prepare(rawValue) }
            .onFailure { DiagnosticLog.warning(DiagnosticEvent.URI_PREPARATION_FAILED) }
            .getOrNull()
            ?: return Result.INVALID_URI

        val result = startActivity(context, prepared.intent)
        if (result == Result.OPENED || prepared.browserFallbackUrl == null) {
            return result
        }

        val fallback = runCatching { prepare(prepared.browserFallbackUrl) }
            .onFailure { DiagnosticLog.warning(DiagnosticEvent.URI_FALLBACK_PREPARATION_FAILED) }
            .getOrNull()
            ?: return result

        val fallbackResult = startActivity(context, fallback.intent)
        return if (fallbackResult == Result.OPENED) Result.OPENED else result
    }

    internal fun prepare(rawValue: String): PreparedIntent? {
        val normalizedValue = QRContentClassifier.normalizeForOpening(rawValue) ?: return null
        val contentType = QRContentClassifier.classify(rawValue)

        return if (contentType == QRContentType.INTENT_URI) {
            prepareIntentUri(normalizedValue)
        } else {
            prepareViewIntent(normalizedValue)
        }
    }

    private fun prepareIntentUri(rawValue: String): PreparedIntent? {
        val parsedIntent = Intent.parseUri(rawValue, Intent.URI_INTENT_SCHEME)
        val action = parsedIntent.action ?: Intent.ACTION_VIEW
        if (action != Intent.ACTION_VIEW) return null

        val data = parsedIntent.data ?: return null
        if (!isSafeScheme(data.scheme)) return null

        val safeIntent = Intent(Intent.ACTION_VIEW, data).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            parsedIntent.`package`
                ?.takeIf(packageNamePattern::matches)
                ?.let { setPackage(it) }
        }
        val browserFallbackUrl = parsedIntent.getStringExtra(BROWSER_FALLBACK_URL)
            ?.takeIf { QRContentClassifier.classify(it) == QRContentType.WEB_URL }
            ?.let { QRContentClassifier.normalizeForOpening(it) }

        return PreparedIntent(
            intent = safeIntent,
            browserFallbackUrl = browserFallbackUrl,
        )
    }

    private fun prepareViewIntent(rawValue: String): PreparedIntent? {
        val uri = rawValue.toUri()
        if (!isSafeScheme(uri.scheme)) return null

        return PreparedIntent(
            intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            },
        )
    }

    private fun startActivity(context: Context, intent: Intent): Result {
        val launchIntent = Intent(intent)
        if (context !is Activity) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(launchIntent)
            Result.OPENED
        } catch (_: ActivityNotFoundException) {
            Result.NO_HANDLER
        } catch (_: SecurityException) {
            DiagnosticLog.warning(DiagnosticEvent.URI_HANDLER_REJECTED)
            Result.FAILED
        } catch (_: RuntimeException) {
            DiagnosticLog.warning(DiagnosticEvent.URI_LAUNCH_FAILED)
            Result.FAILED
        }
    }

    private fun isSafeScheme(scheme: String?): Boolean {
        val normalizedScheme = scheme?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        return normalizedScheme !in blockedSchemes
    }
}
