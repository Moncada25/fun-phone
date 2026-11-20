package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallScreeningPolicyTest {
    private val allRules = CallScreeningSettings(
        blockListedNumbers = true,
        blockHiddenNumbers = true,
        blockNumbersNotInContacts = true,
    )

    private fun screen(
        settings: CallScreeningSettings,
        number: String? = "3105550101",
        isIncoming: Boolean = true,
        presentationAllowed: Boolean = true,
        onBlockedList: Boolean = false,
        inContacts: Boolean? = true,
        verification: CallerVerification = CallerVerification.NOT_VERIFIED,
        recentlyDialed: Boolean = false,
        repeatedCall: Boolean = false,
        resemblesOwn: Boolean = false,
    ) = CallScreeningPolicy.screen(
        settings = settings,
        isIncoming = isIncoming,
        number = number,
        presentationAllowed = presentationAllowed,
        isOnBlockedList = { onBlockedList },
        isInContacts = { inContacts },
        verification = verification,
        wasRecentlyDialed = { recentlyDialed },
        isRepeatedCall = { repeatedCall },
        resemblesOwnNumber = { resemblesOwn },
    )

    @Test
    fun everyRuleIsOffByDefault() {
        val defaults = CallScreeningSettings()

        assertFalse(defaults.anyRuleEnabled)
        assertEquals(0, defaults.enabledRuleCount)
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(defaults, number = null, presentationAllowed = false, onBlockedList = true, inContacts = false),
        )
    }

    @Test
    fun outgoingCallsAreNeverBlocked() {
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(allRules, isIncoming = false, onBlockedList = true, inContacts = false),
        )
    }

    @Test
    fun hiddenNumbersFollowOnlyTheirOwnRule() {
        assertEquals(
            CallScreeningVerdict.BLOCK_HIDDEN,
            screen(allRules, number = "3105550101", presentationAllowed = false),
        )
        assertEquals(CallScreeningVerdict.BLOCK_HIDDEN, screen(allRules, number = ""))
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(
                settings = allRules.copy(blockHiddenNumbers = false),
                number = null,
                presentationAllowed = false,
                onBlockedList = true,
                inContacts = false,
            ),
        )
    }

    @Test
    fun blockedListRuleWinsOverTheContactsRule() {
        assertEquals(
            CallScreeningVerdict.BLOCK_LISTED,
            screen(allRules, onBlockedList = true, inContacts = false),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(CallScreeningSettings(blockHiddenNumbers = true), onBlockedList = true),
        )
    }

    @Test
    fun unknownCallersAreBlockedOnlyWhenTheLookupIsConclusive() {
        val onlyContacts = CallScreeningSettings(blockNumbersNotInContacts = true)

        assertEquals(CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS, screen(onlyContacts, inContacts = false))
        assertEquals(CallScreeningVerdict.ALLOW, screen(onlyContacts, inContacts = true))
        assertEquals(CallScreeningVerdict.ALLOW, screen(onlyContacts, inContacts = null))
    }

    @Test
    fun lookupsRunOnlyForEnabledRules() {
        var blockedLookups = 0
        var contactLookups = 0

        CallScreeningPolicy.screen(
            settings = CallScreeningSettings(blockHiddenNumbers = true),
            isIncoming = true,
            number = "3105550101",
            presentationAllowed = true,
            isOnBlockedList = { blockedLookups++; true },
            isInContacts = { contactLookups++; false },
        )

        assertEquals(0, blockedLookups)
        assertEquals(0, contactLookups)
    }

    @Test
    fun onlyBlockingVerdictsRejectTheCall() {
        assertFalse(CallScreeningVerdict.ALLOW.blocksCall)
        assertTrue(CallScreeningVerdict.BLOCK_LISTED.blocksCall)
        assertTrue(CallScreeningVerdict.BLOCK_HIDDEN.blocksCall)
        assertTrue(CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS.blocksCall)
        assertTrue(CallScreeningVerdict.BLOCK_FAILED_VERIFICATION.blocksCall)
        assertFalse(CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS.blocksCall)
        assertTrue(CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS.silencesCall)
        assertFalse(CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS.silencesCall)
        assertEquals(2, allRules.copy(blockHiddenNumbers = false).enabledRuleCount)
    }

    @Test
    fun failedCarrierVerificationIsBlockedEvenForContacts() {
        val onlyVerification = CallScreeningSettings(blockFailedVerification = true)

        assertTrue(onlyVerification.anyRuleEnabled)
        assertEquals(1, onlyVerification.enabledRuleCount)
        assertEquals(
            CallScreeningVerdict.BLOCK_FAILED_VERIFICATION,
            screen(onlyVerification, inContacts = true, verification = CallerVerification.FAILED),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(onlyVerification, verification = CallerVerification.NOT_VERIFIED),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(onlyVerification, verification = CallerVerification.PASSED),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(allRules.copy(blockFailedVerification = false), verification = CallerVerification.FAILED),
        )
    }

    @Test
    fun blockedListIsReportedBeforeFailedVerification() {
        assertEquals(
            CallScreeningVerdict.BLOCK_LISTED,
            screen(
                allRules.copy(blockFailedVerification = true),
                onBlockedList = true,
                verification = CallerVerification.FAILED,
            ),
        )
    }

    @Test
    fun unknownCallersCanRingSilentlyInsteadOfBeingRejected() {
        val silence = CallScreeningSettings(
            blockNumbersNotInContacts = true,
            silenceNumbersNotInContacts = true,
        )

        assertEquals(CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS, screen(silence, inContacts = false))
        assertEquals(CallScreeningVerdict.ALLOW, screen(silence, inContacts = true))
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(CallScreeningSettings(silenceNumbersNotInContacts = true), inContacts = false),
        )
    }

    @Test
    fun recentlyDialedNumbersPassOnlyWhenTheExceptionIsOn() {
        val rule = CallScreeningSettings(blockNumbersNotInContacts = true)

        assertEquals(
            CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS,
            screen(rule, inContacts = false, recentlyDialed = true),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(rule.copy(allowRecentlyDialed = true), inContacts = false, recentlyDialed = true),
        )
        assertEquals(
            CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS,
            screen(rule.copy(allowRecentlyDialed = true), inContacts = false, recentlyDialed = false),
        )
    }

    @Test
    fun repeatedCallsPassOnlyWhenTheExceptionIsOn() {
        val rule = CallScreeningSettings(
            blockNumbersNotInContacts = true,
            silenceNumbersNotInContacts = true,
        )

        assertEquals(
            CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS,
            screen(rule, inContacts = false, repeatedCall = true),
        )
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(rule.copy(allowRepeatedCalls = true), inContacts = false, repeatedCall = true),
        )
    }

    @Test
    fun exceptionsDoNotOverrideBlockedListHiddenOrSpoofedCalls() {
        val everything = CallScreeningSettings(
            blockListedNumbers = true,
            blockHiddenNumbers = true,
            blockNumbersNotInContacts = true,
            blockFailedVerification = true,
            allowRecentlyDialed = true,
            allowRepeatedCalls = true,
        )

        assertEquals(
            CallScreeningVerdict.BLOCK_LISTED,
            screen(everything, onBlockedList = true, recentlyDialed = true, repeatedCall = true),
        )
        assertEquals(
            CallScreeningVerdict.BLOCK_FAILED_VERIFICATION,
            screen(
                everything,
                verification = CallerVerification.FAILED,
                recentlyDialed = true,
                repeatedCall = true,
            ),
        )
        assertEquals(
            CallScreeningVerdict.BLOCK_HIDDEN,
            screen(everything, number = null, presentationAllowed = false, recentlyDialed = true),
        )
    }

    @Test
    fun callLogLookupsRunOnlyForUnknownCallersWithTheExceptionOn() {
        var dialedLookups = 0
        var repeatLookups = 0
        val settings = CallScreeningSettings(
            blockNumbersNotInContacts = true,
            allowRecentlyDialed = true,
            allowRepeatedCalls = true,
        )

        CallScreeningPolicy.screen(
            settings = settings,
            isIncoming = true,
            number = "3105550101",
            presentationAllowed = true,
            isOnBlockedList = { false },
            isInContacts = { true },
            wasRecentlyDialed = { dialedLookups++; true },
            isRepeatedCall = { repeatLookups++; true },
        )
        CallScreeningPolicy.screen(
            settings = settings,
            isIncoming = true,
            number = "3105550101",
            presentationAllowed = true,
            isOnBlockedList = { false },
            isInContacts = { false },
            wasRecentlyDialed = { dialedLookups++; true },
            isRepeatedCall = { repeatLookups++; true },
        )

        assertEquals(1, dialedLookups)
        assertEquals(0, repeatLookups)
    }

    @Test
    fun numbersThatResembleTheOwnNumberAreBlockedUnlessSaved() {
        val neighbor = CallScreeningSettings(blockNeighborNumbers = true)

        assertTrue(neighbor.anyRuleEnabled)
        assertEquals(1, neighbor.enabledRuleCount)
        assertTrue(CallScreeningVerdict.BLOCK_NEIGHBOR_NUMBER.blocksCall)
        assertEquals(
            CallScreeningVerdict.BLOCK_NEIGHBOR_NUMBER,
            screen(neighbor, inContacts = false, resemblesOwn = true),
        )
        assertEquals(
            CallScreeningVerdict.BLOCK_NEIGHBOR_NUMBER,
            screen(neighbor, inContacts = null, resemblesOwn = true),
        )
        assertEquals(CallScreeningVerdict.ALLOW, screen(neighbor, inContacts = true, resemblesOwn = true))
        assertEquals(CallScreeningVerdict.ALLOW, screen(neighbor, inContacts = false, resemblesOwn = false))
        assertEquals(
            CallScreeningVerdict.ALLOW,
            screen(allRules.copy(blockNumbersNotInContacts = false), inContacts = false, resemblesOwn = true),
        )
    }

    @Test
    fun neighborRuleWinsOverSilencingAndExceptionsForUnknownCallers() {
        val settings = CallScreeningSettings(
            blockNeighborNumbers = true,
            blockNumbersNotInContacts = true,
            silenceNumbersNotInContacts = true,
            allowRepeatedCalls = true,
        )

        assertEquals(
            CallScreeningVerdict.BLOCK_NEIGHBOR_NUMBER,
            screen(settings, inContacts = false, resemblesOwn = true, repeatedCall = true),
        )
        assertEquals(
            CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS,
            screen(settings, inContacts = false, resemblesOwn = false),
        )
    }

    @Test
    fun contactsAreLookedUpOnceAcrossRules() {
        var contactLookups = 0

        CallScreeningPolicy.screen(
            settings = CallScreeningSettings(blockNeighborNumbers = true, blockNumbersNotInContacts = true),
            isIncoming = true,
            number = "3105550101",
            presentationAllowed = true,
            isOnBlockedList = { false },
            isInContacts = { contactLookups++; false },
            resemblesOwnNumber = { false },
        )

        assertEquals(1, contactLookups)
    }

    @Test
    fun everyOptionMapsToItsOwnSetting() {
        CallScreeningOption.entries.forEach { option ->
            val enabled = CallScreeningOption.entries
                .filter { CallScreeningSettings().withOption(it).isEnabled(option) }

            assertEquals(listOf(option), enabled)
        }
    }

    private fun CallScreeningSettings.withOption(option: CallScreeningOption) = when (option) {
        CallScreeningOption.BLOCK_LISTED -> copy(blockListedNumbers = true)
        CallScreeningOption.BLOCK_HIDDEN -> copy(blockHiddenNumbers = true)
        CallScreeningOption.BLOCK_FAILED_VERIFICATION -> copy(blockFailedVerification = true)
        CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS -> copy(blockNeighborNumbers = true)
        CallScreeningOption.BLOCK_NOT_IN_CONTACTS -> copy(blockNumbersNotInContacts = true)
        CallScreeningOption.SILENCE_NOT_IN_CONTACTS -> copy(silenceNumbersNotInContacts = true)
        CallScreeningOption.ALLOW_RECENTLY_DIALED -> copy(allowRecentlyDialed = true)
        CallScreeningOption.ALLOW_REPEATED_CALLS -> copy(allowRepeatedCalls = true)
    }
}
