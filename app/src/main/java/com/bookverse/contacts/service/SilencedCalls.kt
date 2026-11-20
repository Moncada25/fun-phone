package com.bookverse.contacts.service

import android.os.SystemClock
import com.bookverse.contacts.domain.contacts.BlockedNumberMatchPolicy

internal class SilencedCallLedger(private val ttlMillis: Long = DEFAULT_TTL_MILLIS) {
    private val expiries = mutableMapOf<String, Long>()

    fun mark(number: String, nowMillis: Long) {
        prune(nowMillis)
        expiries[number] = nowMillis + ttlMillis
    }

    fun consume(number: String, nowMillis: Long): Boolean {
        prune(nowMillis)
        val match = expiries.keys.firstOrNull { BlockedNumberMatchPolicy.isSameSubscriber(it, number) }
            ?: return false
        expiries.remove(match)
        return true
    }

    private fun prune(nowMillis: Long) {
        expiries.entries.removeAll { (_, expiresAt) -> expiresAt <= nowMillis }
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 60_000L
    }
}

object SilencedCalls {
    private val ledger = SilencedCallLedger()

    @Synchronized
    fun mark(number: String) = ledger.mark(number, SystemClock.elapsedRealtime())

    @Synchronized
    fun consume(number: String): Boolean = ledger.consume(number, SystemClock.elapsedRealtime())
}
