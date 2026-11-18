package com.bookverse.contacts.domain.passwords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownServicesTest {
    private val browser = "org.mozilla.firefox"

    private fun web(domain: String) = CredentialTarget(browser, domain)

    private fun app(packageName: String) = CredentialTarget(packageName, null)

    @Test
    fun catalogUrlsAreValidAndUnique() {
        val ids = KnownServices.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        KnownServices.all.forEach { service ->
            assertTrue(service.id, service.url.startsWith("https://"))
            assertEquals(service.id, service, KnownServices.forHost(service.host))
            assertTrue(service.id, CredentialMatchPolicy.matchesExactly(service.url, web(service.host)))
        }
    }

    @Test
    fun forHostPrefersTheMostSpecificAndPrimaryDomain() {
        assertEquals("amazon", KnownServices.forHost("amazon.com")?.id)
        assertEquals("google", KnownServices.forHost("mail.google.com")?.id)
        assertEquals("microsoft", KnownServices.forHost("login.live.com")?.id)
        assertEquals("apple_tv", KnownServices.forHost("tv.apple.com")?.id)
        assertEquals("apple_id", KnownServices.forHost("account.apple.com")?.id)
        assertNull(KnownServices.forHost("example.com"))
        assertNull(KnownServices.forHost("notnetflix.com"))
    }

    @Test
    fun searchIgnoresCaseAccentsAndMatchesAliases() {
        assertEquals("netflix", KnownServices.search("NETFLIX").single().id)
        assertTrue(KnownServices.search("gmail").any { it.id == "google" })
        assertTrue(KnownServices.search("twitter").any { it.id == "x" })
        assertTrue(KnownServices.search("mercadolibre").any { it.id == "mercado_libre" })
        assertEquals(KnownServices.all, KnownServices.search("  "))
        assertTrue(KnownServices.search("zzzz-none").isEmpty())
    }

    @Test
    fun knownServiceLinksItsAndroidApps() {
        assertTrue(CredentialMatchPolicy.matches("https://www.netflix.com", app("com.netflix.mediaclient")))
        assertFalse(CredentialMatchPolicy.matchesExactly("https://www.netflix.com", app("com.netflix.mediaclient")))
        assertFalse(CredentialMatchPolicy.matches("https://www.netflix.com", app("com.netflix.mediaclient.evil")))
        assertFalse(CredentialMatchPolicy.matches("https://www.netflix.com", app("com.spotify.music")))
        assertFalse(CredentialMatchPolicy.matches("https://example.com", app("com.netflix.mediaclient")))
    }

    @Test
    fun knownServiceMatchesItsRelatedSignInDomainsOnly() {
        assertTrue(CredentialMatchPolicy.matches("https://www.youtube.com", web("accounts.google.com")))
        assertTrue(CredentialMatchPolicy.matches("https://x.com", web("twitter.com")))
        assertTrue(CredentialMatchPolicy.matches("https://www.playstation.com", web("my.account.sony.com")))
        assertFalse(CredentialMatchPolicy.matches("https://www.playstation.com", web("sony.com")))
        assertFalse(CredentialMatchPolicy.matches("https://www.netflix.com", web("netflix.com.evil.org")))
        assertFalse(CredentialMatchPolicy.matches("https://www.amazon.com", web("primevideo.com")))
    }

    @Test
    fun forTargetNamesKnownAppsAndSites() {
        assertEquals("Spotify", KnownServices.forTarget(app("com.spotify.music"))?.name)
        assertEquals("GitHub", KnownServices.forTarget(web("github.com"))?.name)
        assertNull(KnownServices.forTarget(app("com.example.app")))
    }
}
