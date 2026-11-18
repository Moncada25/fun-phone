package com.bookverse.contacts.domain.call

import androidx.compose.runtime.Immutable

@Immutable
data class SimAccount(
    val id: String,
    val label: String,
    val detail: String? = null,
)

sealed interface SimPreference {
    data object AskEveryTime : SimPreference

    data class Account(val id: String) : SimPreference

    fun toStoredValue(): String? = when (this) {
        AskEveryTime -> null
        is Account -> id
    }

    companion object {
        fun fromStoredValue(value: String?): SimPreference =
            value?.takeIf { it.isNotBlank() }?.let(::Account) ?: AskEveryTime
    }
}

sealed interface SimDecision {
    data object SystemDefault : SimDecision

    data class Use(val accountId: String) : SimDecision

    data object Ask : SimDecision
}

object SimSelectionPolicy {
    fun decide(
        accounts: List<SimAccount>,
        preference: SimPreference,
        isEmergencyNumber: Boolean = false,
    ): SimDecision = when {
        isEmergencyNumber || accounts.size < 2 -> SimDecision.SystemDefault
        preference is SimPreference.Account && accounts.any { it.id == preference.id } ->
            SimDecision.Use(preference.id)
        else -> SimDecision.Ask
    }

    fun effectivePreference(
        accounts: List<SimAccount>,
        preference: SimPreference,
    ): SimPreference = when {
        preference is SimPreference.Account && accounts.none { it.id == preference.id } ->
            SimPreference.AskEveryTime
        else -> preference
    }
}
