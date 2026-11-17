package com.bookverse.contacts.data.hangman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class HangmanWordsTest {
    @Test
    fun shouldUseTheSelectedLanguageWordBank() {
        assertEquals("katze", HangmanWords.getWords(HangmanWords.ANIMALS, Locale.GERMAN).first())
        assertEquals("chat", HangmanWords.getWords(HangmanWords.ANIMALS, Locale.FRENCH).first())
        assertEquals("gato", HangmanWords.getWords(HangmanWords.ANIMALS, Locale("pt", "BR")).first())
    }

    @Test
    fun shouldProvideEveryCategoryForSupportedLocales() {
        listOf(Locale.ENGLISH, Locale("es", "ES"), Locale.GERMAN, Locale.FRENCH, Locale("pt", "BR")).forEach { locale ->
            HangmanWords.categories.forEach { category ->
                assertFalse(HangmanWords.getWords(category, locale).isEmpty())
            }
        }
    }
}
