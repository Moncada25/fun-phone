package com.bookverse.contacts.domain.review

import java.time.Duration

data class ReviewEligibilitySnapshot(
    val completedCallDays: Int,
    val lastPromptAtMillis: Long,
    val nowMillis: Long,
    val isDefaultDialer: Boolean,
)

object ReviewEligibilityPolicy {
    const val REQUIRED_DISTINCT_CALL_DAYS = 3
    val PROMPT_COOLDOWN_MILLIS: Long = Duration.ofDays(180).toMillis()

    fun isEligible(snapshot: ReviewEligibilitySnapshot): Boolean {
        if (!snapshot.isDefaultDialer) return false
        if (snapshot.completedCallDays < REQUIRED_DISTINCT_CALL_DAYS) return false
        if (snapshot.lastPromptAtMillis <= 0L) return true
        return snapshot.nowMillis - snapshot.lastPromptAtMillis >= PROMPT_COOLDOWN_MILLIS
    }
}
