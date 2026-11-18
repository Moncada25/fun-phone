package com.bookverse.contacts.domain.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SocialLinkPolicyTest {
    @Test
    fun acceptsHandlesAndProviderUrlsButNeverTheHomeAsAProfile() {
        assertEquals("https://www.instagram.com/user.name", SocialLinkPolicy.profile("instagram.com", "@user.name"))
        assertEquals("https://www.linkedin.com/in/example", SocialLinkPolicy.profile("linkedin.com", "https://linkedin.com/in/example/?trk=test"))
        assertNull(SocialLinkPolicy.profile("facebook.com", ""))
        assertNull(SocialLinkPolicy.profile("instagram.com", "https://instagram.com/"))
        assertNull(SocialLinkPolicy.profile("instagram.com", "instagram.com"))
        assertNull(SocialLinkPolicy.profile("instagram.com", "www.instagram.com"))
        assertNull(SocialLinkPolicy.profile("instagram.com", "accounts"))
    }

    @Test
    fun rejectsDangerousSchemesUserInfoAndImpersonatedHosts() {
        for (url in listOf("http://example.com", "javascript:alert(1)", "file:///tmp/data", "content://contacts/1", "https://good.com@evil.com", "https://example.com:8080", "https://example.com/a b")) {
            assertNull(url, SocialLinkPolicy.website(url))
        }
        assertNull(SocialLinkPolicy.profile("facebook.com", "https://facebook.com.evil.example.org/user"))
        assertFalse(SocialLinkPolicy.isDomain("https://evil.example.org/facebook.com", "facebook.com"))
    }

    @Test
    fun normalizesHttpsAndPreservesEncodedPaths() {
        assertEquals("https://example.com/path?q=1", SocialLinkPolicy.website("example.com/path?q=1"))
        assertEquals("https://www.facebook.com/", SocialLinkPolicy.home("facebook.com"))
        assertNull(SocialLinkPolicy.profile("instagram.com", "https://instagram.com/a%2Fb"))
    }

    @Test
    fun rejectsLocalHostsAndIpLiteralsForPagesAndSubresources() {
        for (url in listOf("https://localhost", "https://localhost.", "https://127.0.0.1", "https://192.168.0.1", "https://[::1]", "https://router.local", "https://private.internal", "https://2130706433")) {
            assertNull(url, SocialLinkPolicy.website(url))
        }
    }
}
