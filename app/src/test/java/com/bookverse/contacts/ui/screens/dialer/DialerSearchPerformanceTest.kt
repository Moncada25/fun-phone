package com.bookverse.contacts.ui.screens.dialer

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class DialerSearchPerformanceTest {
    @Test
    fun twoThousandContactQueryStaysWithinBudget() {
        val contacts = (0 until 2_000).map { index ->
            Contact(
                id = index.toString(),
                name = "Synthetic $index",
                phones = listOf(
                    ContactPhone(
                        number = "555-${index.toString().padStart(4, '0')}",
                        type = null,
                    ),
                ),
            )
        }
        val searchIndex = buildDialerContactSearchIndex(contacts)

        repeat(20) {
            filterDialerContacts(searchIndex, "1999")
        }
        val durationsMs = List(50) {
            measureNanoTime {
                filterDialerContacts(searchIndex, "1999")
            } / 1_000_000.0
        }.sorted()
        val percentile90 = durationsMs[(durationsMs.size * 9 / 10) - 1]
        println("PERF-03 p90_ms=$percentile90 contacts=2000")

        assertTrue(
            "PERF-03 exceeded 100 ms at p90: $percentile90 ms",
            percentile90 <= 100.0,
        )
    }
}
