package com.bookverse.contacts.ui.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AppDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val DEFAULT_CYCLE_LENGTH = 28
const val DEFAULT_LUTEAL_LENGTH = 14
const val DEFAULT_MENSTRUATION_LENGTH = 5
private const val DEFAULT_FERTILE_WINDOW_DAYS = 3
private const val MIN_CYCLE_LENGTH = 21
private const val MAX_CYCLE_LENGTH = 40
private const val MIN_LUTEAL_LENGTH = 8
private const val MAX_LUTEAL_LENGTH = 16

class MenstrualCalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val dataStore = AppDataStore.getDataStore(app)
    private val startDateKey = stringPreferencesKey("menstrual_start_date")
    private val cycleLengthKey = intPreferencesKey("menstrual_cycle_length")
    private val lutealLengthKey = intPreferencesKey("menstrual_luteal_length")
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _uiState = MutableStateFlow(MenstrualCalendarUiState())
    val uiState: StateFlow<MenstrualCalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collectLatest { prefs ->
                val storedStartDate = prefs[startDateKey]?.let { parseDate(it) }
                val cycleLength = prefs[cycleLengthKey] ?: DEFAULT_CYCLE_LENGTH
                val lutealLength = prefs[lutealLengthKey] ?: DEFAULT_LUTEAL_LENGTH
                val startDateError = storedStartDate?.let(::validateStartDate)
                val snapshot = storedStartDate?.takeIf { startDateError == null }?.let {
                    calculateCyclePhases(
                        startDate = it,
                        cycleLength = cycleLength,
                        lutealLength = lutealLength,
                        menstruationLength = DEFAULT_MENSTRUATION_LENGTH
                    )
                }
                _uiState.update { current ->
                    current.copy(
                        selectedStartDate = storedStartDate,
                        cycleLengthInput = cycleLength.toString(),
                        lutealLengthInput = lutealLength.toString(),
                        savedStartDate = storedStartDate,
                        savedCycleLength = prefs[cycleLengthKey]?.takeIf { storedStartDate != null },
                        savedLutealLength = prefs[lutealLengthKey]?.takeIf { storedStartDate != null },
                        snapshot = snapshot,
                        cycleLengthError = null,
                        lutealLengthError = null,
                        startDateError = startDateError,
                        isLoaded = true,
                    )
                }
            }
        }
    }

    fun onStartDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedStartDate = date,
                startDateError = validateStartDate(date),
            )
        }
    }

    fun onCycleLengthChange(input: String) {
        val sanitized = input.filter { it.isDigit() }.take(2)
        _uiState.update {
            it.copy(
                cycleLengthInput = sanitized,
                cycleLengthError = null
            )
        }
    }

    fun onLutealLengthChange(input: String) {
        val sanitized = input.filter { it.isDigit() }.take(2)
        _uiState.update {
            it.copy(
                lutealLengthInput = sanitized,
                lutealLengthError = null
            )
        }
    }

    fun saveConfiguration(): Boolean {
        val state = _uiState.value
        val startDate = state.selectedStartDate
        val cycleLength = state.cycleLengthInput.toIntOrNull()
        val lutealLength = state.lutealLengthInput.toIntOrNull()

        val startDateError = validateStartDate(startDate)
        val cycleError = validateCycleLength(cycleLength)
        val lutealError = validateLutealLength(lutealLength, cycleLength)

        if (startDateError != null || cycleError != null || lutealError != null) {
            _uiState.update {
                it.copy(
                    cycleLengthError = cycleError,
                    lutealLengthError = lutealError,
                    startDateError = startDateError,
                )
            }
            return false
        }

        val snapshot = calculateCyclePhases(
            startDate = requireNotNull(startDate),
            cycleLength = cycleLength!!,
            lutealLength = lutealLength!!,
            menstruationLength = DEFAULT_MENSTRUATION_LENGTH
        )

        _uiState.update {
            it.copy(
                snapshot = snapshot,
                savedStartDate = startDate,
                savedCycleLength = cycleLength,
                savedLutealLength = lutealLength,
                cycleLengthError = null,
                lutealLengthError = null,
                startDateError = null,
            )
        }

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[startDateKey] = startDate.format(formatter)
                prefs[cycleLengthKey] = cycleLength
                prefs[lutealLengthKey] = lutealLength
            }
        }
        return true
    }

    fun resetConfiguration() {
        _uiState.update { current ->
            MenstrualCalendarUiState(isLoaded = current.isLoaded)
        }
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(startDateKey)
                prefs.remove(cycleLengthKey)
                prefs.remove(lutealLengthKey)
            }
        }
    }

    fun discardConfigurationChanges() {
        val state = _uiState.value
        val savedStartDate = state.savedStartDate ?: return
        _uiState.update {
            it.copy(
                selectedStartDate = savedStartDate,
                cycleLengthInput = (state.savedCycleLength ?: DEFAULT_CYCLE_LENGTH).toString(),
                lutealLengthInput = (state.savedLutealLength ?: DEFAULT_LUTEAL_LENGTH).toString(),
                cycleLengthError = null,
                lutealLengthError = null,
                startDateError = validateStartDate(savedStartDate),
            )
        }
    }

    private fun validateStartDate(value: LocalDate?): MenstrualStartDateError? {
        return when {
            value == null -> MenstrualStartDateError.REQUIRED
            value.isAfter(LocalDate.now()) -> MenstrualStartDateError.FUTURE
            else -> null
        }
    }

    private fun validateCycleLength(value: Int?): MenstrualCycleInputError? {
        return when {
            value == null -> MenstrualCycleInputError.INVALID
            value < MIN_CYCLE_LENGTH -> MenstrualCycleInputError.TOO_SHORT
            value > MAX_CYCLE_LENGTH -> MenstrualCycleInputError.TOO_LONG
            else -> null
        }
    }

    private fun validateLutealLength(value: Int?, cycleLength: Int?): MenstrualLutealInputError? {
        return when {
            value == null -> MenstrualLutealInputError.INVALID
            value < MIN_LUTEAL_LENGTH -> MenstrualLutealInputError.TOO_SHORT
            value > MAX_LUTEAL_LENGTH -> MenstrualLutealInputError.TOO_LONG
            cycleLength != null && value >= cycleLength -> MenstrualLutealInputError.AFTER_CYCLE
            else -> null
        }
    }

    private fun parseDate(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value, formatter) }.getOrNull()
    }
}

data class MenstrualCalendarUiState(
    val selectedStartDate: LocalDate? = null,
    val cycleLengthInput: String = DEFAULT_CYCLE_LENGTH.toString(),
    val lutealLengthInput: String = DEFAULT_LUTEAL_LENGTH.toString(),
    val savedStartDate: LocalDate? = null,
    val savedCycleLength: Int? = null,
    val savedLutealLength: Int? = null,
    val snapshot: MenstrualCycleSnapshot? = null,
    val cycleLengthError: MenstrualCycleInputError? = null,
    val lutealLengthError: MenstrualLutealInputError? = null,
    val startDateError: MenstrualStartDateError? = null,
    val isLoaded: Boolean = false,
)

enum class MenstrualStartDateError(@param:StringRes val messageRes: Int) {
    REQUIRED(R.string.menstrual_start_date_error),
    FUTURE(R.string.menstrual_start_date_error_future),
}

enum class MenstrualCycleInputError(@param:StringRes val messageRes: Int) {
    INVALID(R.string.menstrual_cycle_length_error_invalid),
    TOO_SHORT(R.string.menstrual_cycle_length_error_short),
    TOO_LONG(R.string.menstrual_cycle_length_error_long)
}

enum class MenstrualLutealInputError(@param:StringRes val messageRes: Int) {
    INVALID(R.string.menstrual_luteal_error_invalid),
    TOO_SHORT(R.string.menstrual_luteal_error_short),
    TOO_LONG(R.string.menstrual_luteal_error_long),
    AFTER_CYCLE(R.string.menstrual_luteal_error_cycle)
}

data class MenstrualCycleSnapshot(
    val phases: List<MenstrualPhaseDetail>,
    val ovulationDate: LocalDate,
    val nextCycleStart: LocalDate
)

data class MenstrualPhaseDetail(
    val type: MenstrualPhaseType,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val durationDays: Int =
        (ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceAtLeast(0)) + 1
}

enum class MenstrualPhaseType {
    MENSTRUATION,
    FOLLICULAR,
    OVULATION,
    LUTEAL
}

fun calculateCyclePhases(
    startDate: LocalDate,
    cycleLength: Int = DEFAULT_CYCLE_LENGTH,
    lutealLength: Int = DEFAULT_LUTEAL_LENGTH,
    menstruationLength: Int = DEFAULT_MENSTRUATION_LENGTH
): MenstrualCycleSnapshot {
    val normalizedMenstruationLength = menstruationLength.coerceAtLeast(2)
    val fertileWindowLength = DEFAULT_FERTILE_WINDOW_DAYS.coerceAtLeast(1)
    val nextCycleStart = startDate.plusDays(cycleLength.toLong())
    val ovulationDate = nextCycleStart.minusDays(lutealLength.toLong())
    val menstruationEnd = startDate.plusDays((normalizedMenstruationLength - 1).toLong())
    val follicularStart = menstruationEnd.plusDays(1)
    val lutealEnd = nextCycleStart.minusDays(1)
    val fertileOffsetBefore = ((fertileWindowLength - 1) / 2).toLong()
    val fertileStartCandidate = ovulationDate.minusDays(fertileOffsetBefore)
    val fertileStart = maxOf(fertileStartCandidate, follicularStart)
    val fertileEnd = minOf(
        fertileStart.plusDays(fertileWindowLength.toLong() - 1),
        lutealEnd
    )
    val follicularEnd = fertileStart.minusDays(1)
    val lutealStart = fertileEnd.plusDays(1)

    fun createPhase(
        type: MenstrualPhaseType,
        start: LocalDate,
        end: LocalDate
    ): MenstrualPhaseDetail? {
        return if (!end.isBefore(start)) {
            MenstrualPhaseDetail(type = type, startDate = start, endDate = end)
        } else {
            null
        }
    }

    val phases = buildList {
        add(
            MenstrualPhaseDetail(
                type = MenstrualPhaseType.MENSTRUATION,
                startDate = startDate,
                endDate = menstruationEnd
            )
        )
        createPhase(
            type = MenstrualPhaseType.FOLLICULAR,
            start = follicularStart,
            end = follicularEnd
        )?.let { add(it) }
        createPhase(
            type = MenstrualPhaseType.OVULATION,
            start = fertileStart,
            end = fertileEnd
        )?.let { add(it) }
        createPhase(
            type = MenstrualPhaseType.LUTEAL,
            start = lutealStart,
            end = lutealEnd
        )?.let { add(it) }
    }

    return MenstrualCycleSnapshot(
        phases = phases,
        ovulationDate = ovulationDate,
        nextCycleStart = nextCycleStart
    )
}
