package com.bookverse.contacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrTemplatesTest {
    @Test
    fun buildsWpaPayload() {
        assertEquals(
            "WIFI:T:WPA;S:HomeNet;P:correct horse;;",
            QrTemplates.wifi("HomeNet", WifiSecurity.WPA, "correct horse", hidden = false),
        )
    }

    @Test
    fun escapesReservedCharactersInSsidAndPassword() {
        assertEquals(
            "WIFI:T:WPA;S:Caf\\;e\\, \\\"Bar\\\"\\: 5G\\\\;P:p\\;a\\,s\\:s\\\\w\\\"ord;;",
            QrTemplates.wifi("Caf;e, \"Bar\": 5G\\", WifiSecurity.WPA, "p;a,s:s\\w\"ord", hidden = false),
        )
        assertEquals("a\\\\b\\;c\\,d\\:e\\\"f", QrTemplates.escapeWifi("a\\b;c,d:e\"f"))
    }

    @Test
    fun keepsUnicodeAndSpacesUntouched() {
        assertEquals(
            "WIFI:T:WPA;S:Señal de Ñandú 📶;P:contraseña larga;;",
            QrTemplates.wifi("Señal de Ñandú 📶", WifiSecurity.WPA, "contraseña larga", hidden = false),
        )
    }

    @Test
    fun marksHiddenNetworksAndOmitsPasswordForOpenNetworks() {
        assertEquals(
            "WIFI:T:nopass;S:Guest;H:true;;",
            QrTemplates.wifi("Guest", WifiSecurity.OPEN, "ignored", hidden = true),
        )
        assertEquals(
            "WIFI:T:WEP;S:Old;P:wepkey;;",
            QrTemplates.wifi("Old", WifiSecurity.WEP, "wepkey", hidden = false),
        )
    }

    @Test
    fun quotesValuesThatCouldBeReadAsHex() {
        assertEquals(
            "WIFI:T:WPA;S:\"CAFE\";P:\"12345678\";;",
            QrTemplates.wifi("CAFE", WifiSecurity.WPA, "12345678", hidden = false),
        )
    }

    @Test
    fun rejectsInvalidNetworks() {
        assertNull(QrTemplates.wifi("", WifiSecurity.WPA, "longenough", hidden = false))
        assertNull(QrTemplates.wifi("Net", WifiSecurity.WPA, "short", hidden = false))
        assertNull(QrTemplates.wifi("Net", WifiSecurity.WPA, "x".repeat(64), hidden = false))
        assertNull(QrTemplates.wifi("Net", WifiSecurity.WEP, "", hidden = false))
        assertNull(QrTemplates.wifi("n".repeat(33), WifiSecurity.OPEN, "", hidden = false))
        assertTrue(QrTemplates.isWifiPasswordValid(WifiSecurity.WPA, "a".repeat(64)))
        assertTrue(QrTemplates.isWifiPasswordValid(WifiSecurity.WPA, "x".repeat(63)))
        assertFalse(QrTemplates.isWifiPasswordValid(WifiSecurity.WPA, "1234567"))
    }

    @Test
    fun wifiPayloadIsNotTreatedAsAnOpenableLink() {
        val payload = QrTemplates.wifi("HomeNet", WifiSecurity.WPA, "correct horse", hidden = false)!!
        assertEquals(QRContentType.TEXT, QRContentClassifier.classify(payload))
    }

    @Test
    fun buildsMailtoWithEncodedSubjectAndBody() {
        assertEquals("mailto:ana@example.com", QrTemplates.email(" ana@example.com ", "", ""))
        assertEquals(
            "mailto:ana@example.com?subject=Hola%20%26%20adi%C3%B3s&body=l%C3%ADnea%201%0Al%C3%ADnea%202%3F",
            QrTemplates.email("ana@example.com", "Hola & adiós", "línea 1\nlínea 2?"),
        )
        assertNull(QrTemplates.email("not-an-email", "x", "y"))
        assertNull(QrTemplates.email("a b@example.com", "", ""))
    }

    @Test
    fun buildsTelLinks() {
        assertEquals("tel:+573001234567", QrTemplates.phone(" +57 (300) 123-4567 "))
        assertEquals("tel:*123%23", QrTemplates.phone("*123#"))
        assertNull(QrTemplates.phone("12"))
        assertNull(QrTemplates.phone("abc"))
    }
}
