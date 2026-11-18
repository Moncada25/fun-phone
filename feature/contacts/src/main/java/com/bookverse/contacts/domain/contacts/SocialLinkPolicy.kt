package com.bookverse.contacts.domain.contacts

import java.net.URI
import java.net.URLEncoder
import java.util.Locale

/** Only user-requested HTTPS pages may enter the embedded preview. */
object SocialLinkPolicy {
    fun website(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank() || value.any { it.isWhitespace() || it.isISOControl() }) return null
        val candidate = if ("://" in value) value else "https://$value"
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.host.contains('\\') || uri.port !in listOf(-1, 443)
        ) return null
        val host = uri.host.lowercase(Locale.ROOT).trimEnd('.')
        // Embedded previews only accept public host names; never IP literals or local names.
        if (!host.contains('.') || host.any { it == ':' || it == '[' || it == ']' } ||
            host.all { it.isDigit() || it == '.' } ||
            listOf("localhost", "local", "internal", "lan", "home", "test", "invalid").any {
                host == it || host.endsWith(".$it")
            }
        ) return null
        return uri.toASCIIString()
    }

    fun isDomain(raw: String, domain: String): Boolean {
        val host = website(raw)?.let { URI(it).host.lowercase(Locale.ROOT) } ?: return false
        return host == domain || host.endsWith(".$domain")
    }

    fun home(domain: String): String = "https://www.$domain/"

    fun profile(domain: String, raw: String): String? {
        val input = raw.trim().removePrefix("@")
        if (input.isBlank()) return null
        val username = if (input.contains("://") || input.contains('/') || input.equals(domain, true) || input.equals("www.$domain", true)) {
            val url = website(input) ?: return null
            val uri = URI(url)
            val host = uri.host.lowercase(Locale.ROOT)
            if (host != domain && !host.endsWith(".$domain")) return null
            val parts = uri.path.trim('/').split('/')
            if (domain == "linkedin.com") {
                if (parts.size != 2 || parts[0] != "in") return null
                parts[1]
            } else {
                if (parts.size != 1) return null
                parts[0]
            }
        } else input
        if (!username.matches(Regex("[\\p{L}\\p{N}._-]+")) ||
            username.lowercase(Locale.ROOT) in setOf("login", "accounts", "search", "explore", "signup")
        ) return null
        val encoded = URLEncoder.encode(username, "UTF-8").replace("+", "%20")
        val prefix = if (domain == "linkedin.com") "in/" else ""
        return "${home(domain)}$prefix$encoded"
    }
}
