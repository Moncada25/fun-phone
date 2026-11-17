package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PomodoroTimerState {
    IDLE,
    WORK,
    BREAK,
    PAUSED
}

data class PomodoroSettings(
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4
)

class PomodoroViewModel : ViewModel() {
    private val _settings = MutableStateFlow(PomodoroSettings())
    val settings: StateFlow<PomodoroSettings> = _settings.asStateFlow()

    private val _timerState = MutableStateFlow(PomodoroTimerState.IDLE)
    val timerState: StateFlow<PomodoroTimerState> = _timerState.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _completedSessions = MutableStateFlow(0)
    val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()

    private var timerJob: Job? = null

    fun updateSettings(newSettings: PomodoroSettings) {
        _settings.value = newSettings
        if (_timerState.value == PomodoroTimerState.IDLE) {
            _timeLeftSeconds.value = newSettings.workMinutes * 60
            _totalSeconds.value = newSettings.workMinutes * 60
        }
    }

    fun startWorkSession() {
        _timerState.value = PomodoroTimerState.WORK
        _totalSeconds.value = _settings.value.workMinutes * 60
        _timeLeftSeconds.value = _totalSeconds.value
        startTimer()
    }

    fun startBreakSession() {
        val isLongBreak = (_completedSessions.value % _settings.value.sessionsUntilLongBreak) == 0 
            && _completedSessions.value > 0
        
        _timerState.value = PomodoroTimerState.BREAK
        _totalSeconds.value = if (isLongBreak) {
            _settings.value.longBreakMinutes * 60
        } else {
            _settings.value.breakMinutes * 60
        }
        _timeLeftSeconds.value = _totalSeconds.value
        startTimer()
    }

    fun pauseTimer() {
        _timerState.value = PomodoroTimerState.PAUSED
        timerJob?.cancel()
    }

    fun resumeTimer() {
        val previousState = if (_completedSessions.value == 0) {
            PomodoroTimerState.WORK
        } else {
            PomodoroTimerState.BREAK
        }
        _timerState.value = previousState
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = PomodoroTimerState.IDLE
        _timeLeftSeconds.value = _settings.value.workMinutes * 60
        _totalSeconds.value = _settings.value.workMinutes * 60
        _completedSessions.value = 0
    }

    fun skipSession() {
        timerJob?.cancel()
        when (_timerState.value) {
            PomodoroTimerState.WORK -> {
                _completedSessions.value += 1
                startBreakSession()
            }
            PomodoroTimerState.BREAK -> {
                startWorkSession()
            }
            else -> {}
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeftSeconds.value > 0) {
                delay(1000)
                _timeLeftSeconds.value -= 1
            }
            onTimerComplete()
        }
    }

    private fun onTimerComplete() {
        when (_timerState.value) {
            PomodoroTimerState.WORK -> {
                _completedSessions.value += 1
                _timerState.value = PomodoroTimerState.IDLE
            }
            PomodoroTimerState.BREAK -> {
                _timerState.value = PomodoroTimerState.IDLE
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
