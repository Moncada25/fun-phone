package com.bookverse.contacts.domain.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewEligibilityPolicyTest {
    @Test
    fun shouldBeEligible_whenThreeCallDaysAndNoPreviousPrompt() {
        assertTrue(
            ReviewEligibilityPolicy.isEligible(
                ReviewEligibilitySnapshot(
                    completedCallDays = 3,
                    lastPromptAtMillis = 0L,
                    nowMillis = 1_000L,
                    isDefaultDialer = true,
                ),
            ),
        )
    }

    @Test
    fun shouldNotBeEligible_whenCallsHappenedOnFewerThanThreeDays() {
        assertFalse(
            ReviewEligibilityPolicy.isEligible(
                ReviewEligibilitySnapshot(
                    completedCallDays = 2,
                    lastPromptAtMillis = 0L,
                    nowMillis = 1_000L,
                    isDefaultDialer = true,
                ),
            ),
        )
    }

    @Test
    fun shouldNotBeEligible_whenAppIsNotDefaultDialer() {
        assertFalse(
            ReviewEligibilityPolicy.isEligible(
                ReviewEligibilitySnapshot(
                    completedCallDays = 3,
                    lastPromptAtMillis = 0L,
                    nowMillis = 1_000L,
                    isDefaultDialer = false,
                ),
            ),
        )
    }

    @Test
    fun shouldRespectCooldown_whenPromptWasRequestedRecently() {
        val lastPromptAt = 10_000L
        assertFalse(
            ReviewEligibilityPolicy.isEligible(
                ReviewEligibilitySnapshot(
                    completedCallDays = 8,
                    lastPromptAtMillis = lastPromptAt,
                    nowMillis = lastPromptAt + ReviewEligibilityPolicy.PROMPT_COOLDOWN_MILLIS - 1L,
                    isDefaultDialer = true,
                ),
            ),
        )
        assertTrue(
            ReviewEligibilityPolicy.isEligible(
                ReviewEligibilitySnapshot(
                    completedCallDays = 8,
                    lastPromptAtMillis = lastPromptAt,
                    nowMillis = lastPromptAt + ReviewEligibilityPolicy.PROMPT_COOLDOWN_MILLIS,
                    isDefaultDialer = true,
                ),
            ),
        )
    }
}
