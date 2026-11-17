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
    ) = CallScreeningPolicy.screen(
        settings = settings,
        isIncoming = isIncoming,
        number = number,
        presentationAllowed = presentationAllowed,
        isOnBlockedList = { onBlockedList },
        isInContacts = { inContacts },
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
        assertEquals(2, allRules.copy(blockHiddenNumbers = false).enabledRuleCount)
    }
}
