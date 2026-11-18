package com.bookverse.contacts.domain.holidays

import com.bookverse.contacts.data.model.PublicHoliday

interface HolidaySource {
    fun hasInternetConnection(): Boolean

    suspend fun getHolidays(year: Int, countryCode: String): Result<List<PublicHoliday>>
}

sealed class HolidaySourceException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class ClientError(val code: Int) : HolidaySourceException(
        message = "Client error ($code)",
    )

    data class ServerError(val code: Int) : HolidaySourceException(
        message = "Server error ($code)",
    )

    data class Unknown(
        val detail: String? = null,
        val original: Throwable? = null,
    ) : HolidaySourceException(
        message = detail ?: original?.message,
        cause = original,
    )
}
