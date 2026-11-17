package com.bookverse.contacts.settings

import com.bookverse.contacts.ui.viewmodel.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleControllerTest {
    @Test
    fun shouldReadLegacyEnumNamesAndStableTags() {
        assertEquals(AppLanguage.SPANISH, LocaleController.languageForStoredValue("SPANISH"))
        assertEquals(AppLanguage.GERMAN, LocaleController.languageForStoredValue("de-DE"))
        assertEquals(AppLanguage.PORTUGUESE, LocaleController.languageForStoredValue("pt"))
        assertEquals(AppLanguage.SYSTEM, LocaleController.languageForStoredValue("SYSTEM"))
    }

    @Test
    fun shouldFallbackUnknownLanguageToSystem() {
        assertEquals(AppLanguage.SYSTEM, LocaleController.languageForStoredValue("it"))
        assertEquals(AppLanguage.SYSTEM, LocaleController.languageForStoredValue(null))
    }

    @Test
    fun shouldExposeStableTagsAndApplicationLocales() {
        assertEquals("de", LocaleController.stableLanguageTag(AppLanguage.GERMAN))
        assertEquals("system", LocaleController.stableLanguageTag(AppLanguage.SYSTEM))
        assertEquals("fr", LocaleController.localeListFor(AppLanguage.FRENCH).toLanguageTags())
        assertTrue(LocaleController.localeListFor(AppLanguage.SYSTEM).isEmpty)
    }
}
