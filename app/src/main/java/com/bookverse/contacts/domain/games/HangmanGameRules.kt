package com.bookverse.contacts.domain.games

const val HANGMAN_MAX_MISTAKES = 7
val HANGMAN_ALPHABET: List<Char> = ('a'..'z').toList()

fun isHangmanAsciiLetter(letter: Char): Boolean =
    letter in 'a'..'z' || letter in 'A'..'Z'

enum class HangmanGuessOutcome {
    CORRECT,
    INCORRECT,
    DUPLICATE,
}

data class HangmanGuessState(
    val guessedLetters: Set<Char> = emptySet(),
    val mistakes: Int = 0,
)

data class HangmanGuessResult(
    val state: HangmanGuessState,
    val outcome: HangmanGuessOutcome,
)

fun guessHangmanLetter(
    targetWord: String,
    state: HangmanGuessState,
    letter: Char,
    isHardMode: Boolean,
): HangmanGuessResult {
    require(isHangmanAsciiLetter(letter)) { "Hangman guesses must use the A-Z alphabet" }

    val normalizedLetter = letter.lowercaseChar()
    if (normalizedLetter in state.guessedLetters) {
        val nextMistakes = if (isHardMode) {
            (state.mistakes + 1).coerceAtMost(HANGMAN_MAX_MISTAKES)
        } else {
            state.mistakes
        }
        return HangmanGuessResult(
            state = state.copy(mistakes = nextMistakes),
            outcome = HangmanGuessOutcome.DUPLICATE,
        )
    }

    val isCorrect = targetWord.any { it.lowercaseChar() == normalizedLetter }
    val nextMistakes = if (isCorrect) {
        state.mistakes
    } else {
        (state.mistakes + 1).coerceAtMost(HANGMAN_MAX_MISTAKES)
    }
    return HangmanGuessResult(
        state = HangmanGuessState(
            guessedLetters = state.guessedLetters + normalizedLetter,
            mistakes = nextMistakes,
        ),
        outcome = if (isCorrect) {
            HangmanGuessOutcome.CORRECT
        } else {
            HangmanGuessOutcome.INCORRECT
        },
    )
}

fun isHangmanWon(targetWord: String, guessedLetters: Set<Char>): Boolean {
    val letters = targetWord.filterNot(Char::isWhitespace)
    return letters.isNotEmpty() && letters.all { letter ->
        guessedLetters.contains(letter.lowercaseChar())
    }
}

fun isHangmanLost(mistakes: Int): Boolean = mistakes >= HANGMAN_MAX_MISTAKES
