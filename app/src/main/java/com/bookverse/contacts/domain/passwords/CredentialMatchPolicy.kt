package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential
import java.net.URI
import java.util.Locale

data class CredentialTarget(
    val packageName: String,
    val webDomain: String?,
)

object CredentialMatchPolicy {
    const val APP_SCHEME = "androidapp://"

    fun appUrl(packageName: String): String = APP_SCHEME + packageName

    fun targetUrl(target: CredentialTarget): String =
        hostOf(target.webDomain)?.let { "https://$it" } ?: appUrl(target.packageName)

    fun hostOf(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty() || value.startsWith(APP_SCHEME, ignoreCase = true)) return null
        val candidate = if ("://" in value) value else "https://$value"
        val host = runCatching { URI(candidate).host }.getOrNull() ?: return null
        return host.lowercase(Locale.ROOT).removePrefix("www.").trimEnd('.').takeIf { it.isNotEmpty() }
    }

    fun webUrl(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty() || value.startsWith(APP_SCHEME, ignoreCase = true)) return null
        val candidate = if ("://" in value) value else "https://$value"
        val parsed = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase(Locale.ROOT)
        if (scheme != "https" && scheme != "http") return null
        return candidate.takeIf { !parsed.host.isNullOrEmpty() }
    }

    fun matches(storedUrl: String?, target: CredentialTarget): Boolean {
        val stored = storedUrl?.trim().orEmpty()
        if (stored.isEmpty()) return false
        val requestHost = hostOf(target.webDomain)
        if (requestHost == null) {
            return target.webDomain == null && stored == appUrl(target.packageName)
        }
        val storedHost = hostOf(stored) ?: return false
        if ('.' !in storedHost && storedHost != "localhost") return false
        return requestHost == storedHost || requestHost.endsWith(".$storedHost")
    }

    fun matching(
        credentials: List<PasswordCredential>,
        target: CredentialTarget,
    ): List<PasswordCredential> =
        credentials.filter { !it.isDeleted && matches(it.url, target) }
}
