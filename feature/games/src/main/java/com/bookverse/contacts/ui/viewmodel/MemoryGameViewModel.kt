package com.bookverse.contacts.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.ui.model.CardState
import com.bookverse.contacts.ui.model.MemoryCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class CardTheme { Emoji, Animals, Food, Sports, Nature }

enum class BoardSize(val pairs: Int, val columns: Int) {
    SMALL(8, 4),
    LARGE(18, 6)
}

@Immutable
data class MemoryGameUiState(
    val cards: List<MemoryCard> = emptyList(),
    val moves: Int = 0,
    val isGameWon: Boolean = false,
    val isBoardLocked: Boolean = false,
    val lastMatchedName: String? = null,
)

class MemoryGameViewModel : ViewModel() {
    private var currentTheme = CardTheme.Emoji
    private var currentBoardSize = BoardSize.SMALL

    private val emojiCards = listOf(
        "😀" to "😀",
        "🎉" to "🎉",
        "🔥" to "🔥",
        "❤️" to "❤️",
        "⭐" to "⭐",
        "🎵" to "🎵",
        "🌟" to "🌟",
        "🚀" to "🚀",
        "😎" to "😎",
        "🎮" to "🎮",
        "🌈" to "🌈",
        "💎" to "💎",
        "🎨" to "🎨",
        "🏆" to "🏆",
        "⚡" to "⚡",
        "🌙" to "🌙",
        "☀️" to "☀️",
        "🎭" to "🎭"
    )

    private val animalCards = listOf(
        "🐶" to "🐶",
        "🐱" to "🐱",
        "🐼" to "🐼",
        "🐨" to "🐨",
        "🦁" to "🦁",
        "🐯" to "🐯",
        "🐸" to "🐸",
        "🦊" to "🦊",
        "🐵" to "🐵",
        "🐰" to "🐰",
        "🐻" to "🐻",
        "🐷" to "🐷",
        "🐮" to "🐮",
        "🐔" to "🐔",
        "🦉" to "🦉",
        "🦋" to "🦋",
        "🐙" to "🐙",
        "🦀" to "🦀"
    )

    private val foodCards = listOf(
        "🍕" to "🍕",
        "🍔" to "🍔",
        "🍩" to "🍩",
        "🍦" to "🍦",
        "🍎" to "🍎",
        "🍌" to "🍌",
        "🍓" to "🍓",
        "🥑" to "🥑",
        "🍉" to "🍉",
        "🍇" to "🍇",
        "🍒" to "🍒",
        "🥕" to "🥕",
        "🌽" to "🌽",
        "🍞" to "🍞",
        "🧀" to "🧀",
        "🍰" to "🍰",
        "🍪" to "🍪",
        "🥤" to "🥤"
    )

    private val sportsCards = listOf(
        "⚽" to "⚽",
        "🏀" to "🏀",
        "🎾" to "🎾",
        "🏈" to "🏈",
        "⚾" to "⚾",
        "🎱" to "🎱",
        "🏐" to "🏐",
        "🏓" to "🏓",
        "🏸" to "🏸",
        "🥊" to "🥊",
        "🥋" to "🥋",
        "⛳" to "⛳",
        "🏹" to "🏹",
        "🎯" to "🎯",
        "🥅" to "🥅",
        "⛸️" to "⛸️",
        "🛹" to "🛹",
        "🏋️" to "🏋️"
    )

    private val natureCards = listOf(
        "🌺" to "🌺",
        "🌻" to "🌻",
        "🌹" to "🌹",
        "🌷" to "🌷",
        "🌲" to "🌲",
        "🌴" to "🌴",
        "🌵" to "🌵",
        "🍀" to "🍀",
        "🌾" to "🌾",
        "🌿" to "🌿",
        "🍃" to "🍃",
        "🍂" to "🍂",
        "🍁" to "🍁",
        "🌸" to "🌸",
        "🏵️" to "🏵️",
        "🌼" to "🌼",
        "💐" to "💐",
        "🌱" to "🌱"
    )

    private val _uiState = MutableStateFlow(MemoryGameUiState())
    val uiState: StateFlow<MemoryGameUiState> = _uiState.asStateFlow()

    private var firstSelected: MemoryCard? = null
    private var secondSelected: MemoryCard? = null
    private var flipBackJob: Job? = null

    init {
        resetGame()
    }

    fun setCardTheme(theme: CardTheme) {
        currentTheme = theme
    }

    fun setBoardSize(size: BoardSize) {
        currentBoardSize = size
    }

    fun resetGame() {
        val cardsList = when (currentTheme) {
            CardTheme.Emoji -> emojiCards
            CardTheme.Animals -> animalCards
            CardTheme.Food -> foodCards
            CardTheme.Sports -> sportsCards
            CardTheme.Nature -> natureCards
        }

        val pairList = cardsList.take(currentBoardSize.pairs).flatMap { (name, res) ->
            listOf(
                MemoryCard(Random.nextInt(), name, res, CardState.HIDDEN),
                MemoryCard(Random.nextInt(), name, res, CardState.HIDDEN)
            )
        }

        _uiState.value = MemoryGameUiState(cards = pairList.shuffled())
        firstSelected = null
        secondSelected = null
        flipBackJob?.cancel()
    }

    fun onCardClicked(card: MemoryCard) {
        val state = _uiState.value
        if (state.isBoardLocked || card.state != CardState.HIDDEN || state.isGameWon) return
        val currentCards = state.cards.toMutableList()
        val idx = currentCards.indexOfFirst { it.id == card.id }
        if (idx == -1) return
        currentCards[idx] = card.copy(state = CardState.VISIBLE)
        _uiState.update { it.copy(cards = currentCards) }

        if (firstSelected == null) {
            firstSelected = currentCards[idx]
        } else if (secondSelected == null && card.id != firstSelected?.id) {
            secondSelected = currentCards[idx]
            _uiState.update { it.copy(moves = it.moves + 1) }
            if (firstSelected?.name == secondSelected?.name) {
                markCardsAsMatched(firstSelected!!, secondSelected!!)
            } else {
                _uiState.update { it.copy(isBoardLocked = true) }
                flipBackJob = viewModelScope.launch {
                    delay(1000)
                    hideCards(firstSelected!!, secondSelected!!)
                    _uiState.update { it.copy(isBoardLocked = false) }
                }
            }
        }
    }

    private fun markCardsAsMatched(card1: MemoryCard, card2: MemoryCard) {
        _uiState.update { state ->
            val newCards = state.cards.map {
                if (it.id == card1.id || it.id == card2.id) it.copy(state = CardState.MATCHED) else it
            }
            state.copy(
                cards = newCards,
                lastMatchedName = card1.name,
                isGameWon = newCards.all { it.state == CardState.MATCHED }
            )
        }
        firstSelected = null
        secondSelected = null
    }

    private fun hideCards(card1: MemoryCard, card2: MemoryCard) {
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map {
                    if (it.id == card1.id || it.id == card2.id) it.copy(state = CardState.HIDDEN) else it
                }
            )
        }
        firstSelected = null
        secondSelected = null
    }

    override fun onCleared() {
        super.onCleared()
        flipBackJob?.cancel()
    }
}
