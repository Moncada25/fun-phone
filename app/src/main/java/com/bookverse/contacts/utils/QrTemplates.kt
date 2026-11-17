package com.bookverse.contacts.utils

import java.net.URLEncoder

enum class WifiSecurity(val code: String) {
    WPA("WPA"),
    WEP("WEP"),
    OPEN("nopass"),
}

object QrTemplates {
    private const val WIFI_ESCAPED = "\\;,:\""
    private val hexOnly = Regex("^[0-9A-Fa-f]+$")
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val wpaPassphraseLength = 8..63
    private const val WPA_RAW_KEY_LENGTH = 64

    fun wifi(ssid: String, security: WifiSecurity, password: String, hidden: Boolean): String? {
        if (ssid.isEmpty() || ssid.toByteArray(Charsets.UTF_8).size > 32) return null
        if (!isWifiPasswordValid(security, password)) return null
        return buildString {
            append("WIFI:T:").append(security.code).append(';')
            append("S:").append(wifiField(ssid)).append(';')
            if (security != WifiSecurity.OPEN) append("P:").append(wifiField(password)).append(';')
            if (hidden) append("H:true;")
            append(';')
        }
    }

    fun isWifiPasswordValid(security: WifiSecurity, password: String): Boolean = when (security) {
        WifiSecurity.OPEN -> true
        WifiSecurity.WEP -> password.isNotEmpty()
        WifiSecurity.WPA -> password.length in wpaPassphraseLength ||
            (password.length == WPA_RAW_KEY_LENGTH && hexOnly.matches(password))
    }

    internal fun escapeWifi(value: String): String = buildString(value.length) {
        for (char in value) {
            if (char in WIFI_ESCAPED) append('\\')
            append(char)
        }
    }

    private fun wifiField(value: String): String {
        val escaped = escapeWifi(value)
        return if (hexOnly.matches(value)) "\"$escaped\"" else escaped
    }

    fun email(address: String, subject: String, body: String): String? {
        val recipient = address.trim()
        if (!emailPattern.matches(recipient)) return null
        val query = listOfNotNull(
            subject.takeIf { it.isNotBlank() }?.let { "subject=${percentEncode(it)}" },
            body.takeIf { it.isNotBlank() }?.let { "body=${percentEncode(it)}" },
        ).joinToString("&")
        return if (query.isEmpty()) "mailto:$recipient" else "mailto:$recipient?$query"
    }

    fun phone(number: String): String? {
        val trimmed = number.trim()
        val digits = trimmed.filter { it.isDigit() || it == '*' || it == '#' }
        if (digits.count { it.isDigit() } < 3) return null
        return "tel:" + (if (trimmed.startsWith("+")) "+" else "") + digits.replace("#", "%23")
    }

    private fun percentEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
