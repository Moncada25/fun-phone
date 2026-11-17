package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Test

class SimSelectionPolicyTest {
    private val simOne = SimAccount(id = "telephony#1", label = "Personal")
    private val simTwo = SimAccount(id = "telephony#2", label = "Work")

    @Test
    fun singleSimDevicesAlwaysUseTheSystemDefault() {
        assertEquals(
            SimDecision.SystemDefault,
            SimSelectionPolicy.decide(emptyList(), SimPreference.AskEveryTime),
        )
        assertEquals(
            SimDecision.SystemDefault,
            SimSelectionPolicy.decide(listOf(simOne), SimPreference.AskEveryTime),
        )
        assertEquals(
            SimDecision.SystemDefault,
            SimSelectionPolicy.decide(listOf(simOne), SimPreference.Account(simTwo.id)),
        )
    }

    @Test
    fun multiSimAsksUnlessARememberedSimIsStillAvailable() {
        val accounts = listOf(simOne, simTwo)

        assertEquals(SimDecision.Ask, SimSelectionPolicy.decide(accounts, SimPreference.AskEveryTime))
        assertEquals(
            SimDecision.Use(simTwo.id),
            SimSelectionPolicy.decide(accounts, SimPreference.Account(simTwo.id)),
        )
        assertEquals(
            SimDecision.Ask,
            SimSelectionPolicy.decide(accounts, SimPreference.Account("telephony#removed")),
        )
    }

    @Test
    fun emergencyNumbersNeverWaitForASimChoice() {
        assertEquals(
            SimDecision.SystemDefault,
            SimSelectionPolicy.decide(
                accounts = listOf(simOne, simTwo),
                preference = SimPreference.AskEveryTime,
                isEmergencyNumber = true,
            ),
        )
    }

    @Test
    fun staleRememberedSimIsShownAsAskEveryTime() {
        val accounts = listOf(simOne, simTwo)

        assertEquals(
            SimPreference.AskEveryTime,
            SimSelectionPolicy.effectivePreference(accounts, SimPreference.Account("telephony#removed")),
        )
        assertEquals(
            SimPreference.Account(simOne.id),
            SimSelectionPolicy.effectivePreference(accounts, SimPreference.Account(simOne.id)),
        )
    }

    @Test
    fun preferenceRoundTripsThroughItsStoredValue() {
        assertEquals(SimPreference.AskEveryTime, SimPreference.fromStoredValue(null))
        assertEquals(SimPreference.AskEveryTime, SimPreference.fromStoredValue("  "))
        assertEquals(null, SimPreference.AskEveryTime.toStoredValue())
        assertEquals(
            SimPreference.Account(simOne.id),
            SimPreference.fromStoredValue(SimPreference.Account(simOne.id).toStoredValue()),
        )
    }
}
