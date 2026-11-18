package com.bookverse.contacts.ui.screens.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class HolidayDateFormatterTest {
    @Test
    fun invalidHolidayDateFallsBackToSourceValue() {
        assertEquals(
            "not-a-date",
            HolidayDateFormatter.holiday("not-a-date", Locale.US),
        )
    }

    @Test
    fun validHolidayAndEventRangeProduceReadableLabels() {
        assertTrue(
            HolidayDateFormatter.holiday("2026-07-24", Locale.US)
                .contains("July"),
        )
        assertTrue(
            HolidayDateFormatter.eventTime(0L, 60_000L, Locale.US)
                .contains(" - "),
        )
    }

    @Test
    fun localizedDateFormatsDoNotUseSpanishConnectorsForOtherLocales() {
        val german = HolidayDateFormatter.holiday("2026-07-24", Locale.GERMAN)
        val french = HolidayDateFormatter.holiday("2026-07-24", Locale.FRENCH)
        val portuguese = HolidayDateFormatter.holiday("2026-07-24", Locale("pt", "BR"))

        assertTrue(german.contains("Juli"))
        assertTrue(french.contains("juillet"))
        assertTrue(portuguese.contains("julho"))
        assertTrue(german.contains("de").not())
    }
}
