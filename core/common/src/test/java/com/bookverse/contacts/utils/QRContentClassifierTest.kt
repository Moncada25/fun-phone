package com.bookverse.contacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QRContentClassifierTest {
    @Test
    fun shouldClassifyHttpUrlAsWebUrl() {
        assertEquals(
            QRContentType.WEB_URL,
            QRContentClassifier.classify("https://www.crunchyroll.com/activate"),
        )
    }

    @Test
    fun shouldClassifyCustomSchemeAsAppDeepLink() {
        assertEquals(
            QRContentType.APP_DEEP_LINK,
            QRContentClassifier.classify("crunchyroll://login?code=abc"),
        )
    }

    @Test
    fun shouldClassifyIntentUriAsIntentUri() {
        assertEquals(
            QRContentType.INTENT_URI,
            QRContentClassifier.classify(
                "intent://login#Intent;scheme=crunchyroll;package=com.crunchyroll.crunchyroid;end",
            ),
        )
    }

    @Test
    fun shouldRecognizeSchemelessWebUrl() {
        assertEquals(
            QRContentType.WEB_URL,
            QRContentClassifier.classify("www.crunchyroll.com/activate"),
        )
        assertEquals(
            "https://www.crunchyroll.com/activate",
            QRContentClassifier.normalizeForOpening("www.crunchyroll.com/activate"),
        )
    }

    @Test
    fun shouldUseMlKitUrlHintForSchemelessUrl() {
        assertEquals(
            QRContentType.WEB_URL,
            QRContentClassifier.classify("activate.example", detectedAsUrl = true),
        )
        assertTrue(
            QRContentClassifier.isOpenable("activate.example", detectedAsUrl = true),
        )
    }

    @Test
    fun shouldKeepPlainTextAsNonOpenable() {
        assertEquals(
            QRContentType.TEXT,
            QRContentClassifier.classify("login code 123456"),
        )
        assertFalse(QRContentClassifier.isOpenable("login code 123456"))
        assertEquals(null, QRContentClassifier.normalizeForOpening("login code 123456"))
    }
}
