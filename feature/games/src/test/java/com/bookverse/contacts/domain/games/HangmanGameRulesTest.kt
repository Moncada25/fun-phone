package com.bookverse.contacts.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HangmanGameRulesTest {
    @Test
    fun correctGuessRevealsTheLetterWithoutAddingMistakes() {
        val result = guessHangmanLetter(
            targetWord = "gato",
            state = HangmanGuessState(),
            letter = 'a',
            isHardMode = false,
        )

        assertEquals(setOf('a'), result.state.guessedLetters)
        assertEquals(0, result.state.mistakes)
        assertEquals(HangmanGuessOutcome.CORRECT, result.outcome)
    }

    @Test
    fun incorrectGuessAddsOneMistakeAndRemembersTheLetter() {
        val result = guessHangmanLetter(
            targetWord = "gato",
            state = HangmanGuessState(),
            letter = 'e',
            isHardMode = false,
        )

        assertEquals(setOf('e'), result.state.guessedLetters)
        assertEquals(1, result.state.mistakes)
        assertEquals(HangmanGuessOutcome.INCORRECT, result.outcome)
    }

    @Test
    fun duplicateGuessDoesNotCostAnAttemptInEasyMode() {
        val result = guessHangmanLetter(
            targetWord = "gato",
            state = HangmanGuessState(guessedLetters = setOf('a'), mistakes = 2),
            letter = 'A',
            isHardMode = false,
        )

        assertEquals(setOf('a'), result.state.guessedLetters)
        assertEquals(2, result.state.mistakes)
        assertEquals(HangmanGuessOutcome.DUPLICATE, result.outcome)
    }

    @Test
    fun duplicateGuessCostsAnAttemptInHardMode() {
        val result = guessHangmanLetter(
            targetWord = "gato",
            state = HangmanGuessState(guessedLetters = setOf('a'), mistakes = 2),
            letter = 'a',
            isHardMode = true,
        )

        assertEquals(3, result.state.mistakes)
        assertEquals(HangmanGuessOutcome.DUPLICATE, result.outcome)
    }

    @Test
    fun victoryIgnoresSpacesInTheTargetWord() {
        assertTrue(
            isHangmanWon(
                targetWord = "fun phone",
                guessedLetters = setOf('f', 'u', 'n', 'p', 'h', 'o', 'e'),
            )
        )
    }

    @Test
    fun customWordsUseOnlyTheUnaccentedAlphabet() {
        assertTrue(isHangmanAsciiLetter('A'))
        assertTrue(isHangmanAsciiLetter('z'))
        assertFalse(isHangmanAsciiLetter('ñ'))
        assertFalse(isHangmanAsciiLetter('á'))
        assertEquals(('a'..'z').toList(), HANGMAN_ALPHABET)
    }

    @Test(expected = IllegalArgumentException::class)
    fun accentedGuessIsRejected() {
        guessHangmanLetter(
            targetWord = "nino",
            state = HangmanGuessState(),
            letter = 'á',
            isHardMode = false,
        )
    }

    @Test
    fun lossStartsAtTheSevenMistakeLimit() {
        assertFalse(isHangmanLost(HANGMAN_MAX_MISTAKES - 1))
        assertTrue(isHangmanLost(HANGMAN_MAX_MISTAKES))
    }

    @Test
    fun mistakesAreCappedAtTheConfiguredLimit() {
        val result = guessHangmanLetter(
            targetWord = "gato",
            state = HangmanGuessState(mistakes = HANGMAN_MAX_MISTAKES),
            letter = 'x',
            isHardMode = false,
        )

        assertEquals(HANGMAN_MAX_MISTAKES, result.state.mistakes)
    }
}
