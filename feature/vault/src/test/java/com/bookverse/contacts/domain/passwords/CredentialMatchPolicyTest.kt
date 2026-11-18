package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMatchPolicyTest {
    private val browser = "org.mozilla.firefox"

    private fun web(domain: String) = CredentialTarget(browser, domain)

    @Test
    fun hostIsNormalizedFromLooseInput() {
        assertEquals("example.com", CredentialMatchPolicy.hostOf("https://www.Example.com/login?x=1"))
        assertEquals("example.com", CredentialMatchPolicy.hostOf("example.com"))
        assertEquals("login.example.com", CredentialMatchPolicy.hostOf(" http://login.example.com:8443/a "))
        assertNull(CredentialMatchPolicy.hostOf("androidapp://com.example"))
        assertNull(CredentialMatchPolicy.hostOf(""))
        assertNull(CredentialMatchPolicy.hostOf(null))
        assertNull(CredentialMatchPolicy.hostOf("not a url"))
    }

    @Test
    fun webDomainMatchesSameHostOrSubdomainOfStoredHost() {
        assertTrue(CredentialMatchPolicy.matches("https://example.com/login", web("example.com")))
        assertTrue(CredentialMatchPolicy.matches("example.com", web("www.example.com")))
        assertTrue(CredentialMatchPolicy.matches("example.com", web("accounts.example.com")))
        assertFalse(CredentialMatchPolicy.matches("accounts.example.com", web("example.com")))
        assertFalse(CredentialMatchPolicy.matches("accounts.example.com", web("mail.example.com")))
    }

    @Test
    fun lookalikeAndSuffixDomainsDoNotMatch() {
        assertFalse(CredentialMatchPolicy.matches("example.com", web("badexample.com")))
        assertFalse(CredentialMatchPolicy.matches("example.com", web("example.com.evil.org")))
        assertFalse(CredentialMatchPolicy.matches("example.com", web("example.org")))
        assertFalse(CredentialMatchPolicy.matches("com", web("example.com")))
        assertFalse(CredentialMatchPolicy.matches("", web("example.com")))
        assertFalse(CredentialMatchPolicy.matches(null, web("example.com")))
    }

    @Test
    fun appsMatchOnlyTheirExactPackageLink() {
        val app = CredentialTarget("com.example.app", null)
        assertTrue(CredentialMatchPolicy.matches("androidapp://com.example.app", app))
        assertFalse(CredentialMatchPolicy.matches("androidapp://com.example.app.evil", app))
        assertFalse(CredentialMatchPolicy.matches("androidapp://com.example", app))
        assertFalse(CredentialMatchPolicy.matches("https://example.com", app))
        assertFalse(CredentialMatchPolicy.matches("androidapp://$browser", web("example.com")))
    }

    @Test
    fun targetUrlPrefersWebDomainThenPackage() {
        assertEquals("https://example.com", CredentialMatchPolicy.targetUrl(web("www.example.com")))
        assertEquals(
            "androidapp://com.example.app",
            CredentialMatchPolicy.targetUrl(CredentialTarget("com.example.app", null)),
        )
    }

    @Test
    fun webUrlOnlyAllowsHttpAndHttps() {
        assertEquals("https://example.com", CredentialMatchPolicy.webUrl("example.com"))
        assertEquals("http://example.com/a", CredentialMatchPolicy.webUrl("http://example.com/a"))
        assertNull(CredentialMatchPolicy.webUrl("javascript://example.com/%0aalert(1)"))
        assertNull(CredentialMatchPolicy.webUrl("intent://example.com#Intent;end"))
        assertNull(CredentialMatchPolicy.webUrl("file:///sdcard/a"))
        assertNull(CredentialMatchPolicy.webUrl("androidapp://com.example"))
        assertNull(CredentialMatchPolicy.webUrl(" "))
    }

    @Test
    fun matchingSkipsDeletedEntries() {
        fun credential(id: Long, url: String?, deleted: Boolean = false) = PasswordCredential(
            id = id, name = "n$id", username = "u", password = "p", note = null,
            updatedAt = 0, createdAt = 0, isDeleted = deleted, url = url,
        )
        val all = listOf(
            credential(1, "example.com"),
            credential(2, "example.com", deleted = true),
            credential(3, "other.com"),
            credential(4, null),
        )
        assertEquals(listOf(1L), CredentialMatchPolicy.matching(all, web("example.com")).map { it.id })
    }
}
