package com.bookverse.contacts.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidaysRepositoryTest {

    @Test
    fun successfulResponseIsParsedAndCached() = runTest {
        var requests = 0
        val source = HolidaysRemoteSource { year, countryCode ->
            requests += 1
            assertEquals(2026, year)
            assertEquals("CO", countryCode)
            HolidaysHttpResponse(
                statusCode = 200,
                body = """
                    [
                      {
                        "date": "2026-07-20",
                        "localName": "Día de la Independencia",
                        "name": "Independence Day",
                        "countryCode": "CO",
                        "fixed": true,
                        "global": true,
                        "counties": null,
                        "launchYear": null,
                        "types": ["Public"]
                      }
                    ]
                """.trimIndent(),
            )
        }
        val repository = HolidaysRepository(source, nowMillis = { 1_000L })

        val first = repository.getHolidays(2026, "co").getOrThrow()
        val second = repository.getHolidays(2026, "CO").getOrThrow()

        assertEquals(1, first.size)
        assertEquals("Día de la Independencia", first.single().localName)
        assertEquals(first, second)
        assertEquals(1, requests)
    }

    @Test
    fun httpErrorsUseTypedRepositoryErrors() = runTest {
        val repository = HolidaysRepository(
            remoteSource = HolidaysRemoteSource {
                    _, _ -> HolidaysHttpResponse(statusCode = 503, body = "")
            },
        )

        val exception = repository.getHolidays(2026, "CO").exceptionOrNull()

        assertEquals(HolidaysRepositoryException.ServerError(503), exception)
    }

    @Test
    fun invalidCountryCodeDoesNotReachTheNetwork() = runTest {
        var requested = false
        val repository = HolidaysRepository(
            remoteSource = HolidaysRemoteSource { _, _ ->
                requested = true
                HolidaysHttpResponse(statusCode = 200, body = "[]")
            },
        )

        val exception = repository.getHolidays(2026, "COL").exceptionOrNull()

        assertEquals(HolidaysRepositoryException.ClientError(400), exception)
        assertTrue(!requested)
    }
}
