package com.bookverse.contacts.ui.model

enum class CardState {
    HIDDEN,
    VISIBLE,
    MATCHED
}

data class MemoryCard(
    val id: Int,
    val name: String,
    val resourceName: String,
    val state: CardState
)
