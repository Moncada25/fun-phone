package com.bookverse.contacts.ui.state

sealed class HolidaysUiError {
    data object NoInternet : HolidaysUiError()
    data class Client(val code: Int) : HolidaysUiError()
    data class Server(val code: Int) : HolidaysUiError()
    data class Unknown(val message: String? = null) : HolidaysUiError()
}
