package com.bookverse.contacts.data.repository

sealed class HolidaysRepositoryException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    data class ClientError(val code: Int) : HolidaysRepositoryException(
        message = "Client error ($code)"
    )

    data class ServerError(val code: Int) : HolidaysRepositoryException(
        message = "Server error ($code)"
    )

    data class Unknown(
        val detail: String? = null,
        val original: Throwable? = null
    ) : HolidaysRepositoryException(
        message = detail ?: original?.message,
        cause = original
    )
}
