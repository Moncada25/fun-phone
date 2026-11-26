package com.bookverse.contacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactAvatarLabelTest {
    @Test
    fun usesFirstAndLastName() {
        assertEquals("WB", "Willow Birch".contactAvatarLabel())
        assertEquals("MG", "  maría\tJosé\u00a0Gómez  ".contactAvatarLabel())
        assertEquals("AA", "Ana Ana".contactAvatarLabel())
    }

    @Test
    fun usesOneInitialForOneWord() {
        assertEquals("P", "Policia".contactAvatarLabel())
        assertEquals("É", "  école  ".contactAvatarLabel())
    }

    @Test
    fun preservesUnicodeLettersAndSkipsNonNameTokens() {
        assertEquals("ÁÑ", "📞 álvaro 123 ñíguez".contactAvatarLabel())
        assertEquals("A\u0301𐐀", "a\u0301ngel 𐐨".contactAvatarLabel())
    }

    @Test
    fun keepsPlaceholdersForNamesWithoutLetters() {
        assertNull("+57 123".contactInitials())
        assertNull("  ".contactInitials())
        assertEquals("+", "+57 123".contactAvatarLabel())
        assertEquals("📞", "📞".contactAvatarLabel())
        assertEquals("#", "".contactAvatarLabel())
        assertEquals("?", " ".contactAvatarLabel(emptyLabel = "?"))
    }
}
