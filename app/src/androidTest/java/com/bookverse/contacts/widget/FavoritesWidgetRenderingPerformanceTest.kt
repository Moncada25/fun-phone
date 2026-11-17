package com.bookverse.contacts.widget

import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

@SdkSuppress(minSdkVersion = 31)
class FavoritesWidgetRenderingPerformanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val favorites = (1..12).map { index ->
        Contact(
            id = "synthetic-widget-$index",
            name = "Synthetic Favorite $index",
            phones = listOf(
                ContactPhone(
                    number = "+1555000${index.toString().padStart(4, '0')}",
                    type = null,
                )
            ),
            isFavorite = true,
        )
    }

    @Test
    fun twelveFavoriteWidgetEntriesRenderWithinBudget() {
        repeat(5) {
            render()
        }
        val durationsMs = List(20) {
            measureNanoTime(::render) / 1_000_000.0
        }.sorted()
        val percentile90 = durationsMs[(durationsMs.size * 9 / 10) - 1]
        println("PERF-04 render_p90_ms=$percentile90 favorites=${favorites.size}")

        assertTrue(
            "PERF-04 render stage exceeded 500 ms at p90: $percentile90 ms",
            percentile90 <= 500.0,
        )
    }

    private fun render() {
        val entries = favoriteWidgetEntries(context, favorites)
        assertEquals(favorites.size, entries.size)
    }
}
