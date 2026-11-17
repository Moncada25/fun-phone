package com.bookverse.contacts.utils

enum class QRContentType {
    TEXT,
    WEB_URL,
    APP_DEEP_LINK,
    INTENT_URI;

    val isOpenable: Boolean
        get() = this != TEXT
}

object QRContentClassifier {
    private val schemePattern = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val webUrlPattern = Regex(
        pattern = "^(?:https?://)?(?:www\\.)?(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}(?::\\d{1,5})?(?:[/?#][^\\s]*)?$",
        option = RegexOption.IGNORE_CASE,
    )

    fun classify(content: String, detectedAsUrl: Boolean = false): QRContentType {
        val value = content.trim()
        if (value.isEmpty()) return QRContentType.TEXT

        if (value.startsWith("intent:", ignoreCase = true)) {
            return QRContentType.INTENT_URI
        }

        if (value.startsWith("WIFI:", ignoreCase = true)) {
            return QRContentType.TEXT
        }

        if (detectedAsUrl || isWebUrl(value)) {
            return QRContentType.WEB_URL
        }

        return if (schemePattern.containsMatchIn(value)) {
            QRContentType.APP_DEEP_LINK
        } else {
            QRContentType.TEXT
        }
    }

    fun isOpenable(content: String, detectedAsUrl: Boolean = false): Boolean =
        classify(content, detectedAsUrl).isOpenable

    fun normalizeForOpening(content: String, detectedAsUrl: Boolean = false): String? {
        val value = content.trim()
        if (value.isEmpty()) return null

        return when (classify(value, detectedAsUrl)) {
            QRContentType.WEB_URL -> {
                if (schemePattern.containsMatchIn(value)) value else "https://$value"
            }

            QRContentType.APP_DEEP_LINK,
            QRContentType.INTENT_URI,
            -> value

            QRContentType.TEXT -> null
        }
    }

    private fun isWebUrl(value: String): Boolean = webUrlPattern.matches(value)
}
