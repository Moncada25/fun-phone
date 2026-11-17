package com.bookverse.contacts.domain.contacts

import java.util.concurrent.TimeUnit

object ContactRecycleBinPolicy {
    const val RETENTION_DAYS = 30
    private val dayMillis = TimeUnit.DAYS.toMillis(1)
    private val retentionMillis = RETENTION_DAYS * dayMillis

    fun isExpired(deletedAt: Long, now: Long): Boolean = now - deletedAt >= retentionMillis

    fun daysRemaining(deletedAt: Long, now: Long): Int {
        val remainingMillis = deletedAt + retentionMillis - now
        if (remainingMillis <= 0) return 0
        val days = (remainingMillis + dayMillis - 1) / dayMillis
        return days.coerceAtMost(RETENTION_DAYS.toLong()).toInt()
    }
}
