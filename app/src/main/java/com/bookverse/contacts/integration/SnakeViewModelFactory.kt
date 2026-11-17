package com.bookverse.contacts.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.ui.viewmodel.SnakeViewModel

class SnakeViewModelFactory(
    private val gameStatsRepository: GameStatsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SnakeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return SnakeViewModel(gameStatsRepository) as T
    }
}
