package com.bookverse.contacts.ui.screens.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class GameSessionRecorder(
    private val gameId: GameId,
    private val repository: GameStatsRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sessionIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private var activeSessionId: String? = null

    fun start() {
        val sessionId = sessionIdFactory()
        activeSessionId = sessionId
        scope.launch {
            repository.startSession(gameId, sessionId, clock())
        }
    }

    fun complete(completion: GameCompletion) {
        val sessionId = activeSessionId ?: return
        activeSessionId = null
        scope.launch {
            repository.completeSession(gameId, sessionId, completion)
        }
    }

    fun abandon() {
        activeSessionId = null
    }
}

@Composable
internal fun rememberGameSessionRecorder(
    gameId: GameId,
    repository: GameStatsRepository,
): GameSessionRecorder {
    val scope = rememberCoroutineScope()
    return remember(gameId, repository, scope) {
        GameSessionRecorder(gameId, repository, scope)
    }
}
