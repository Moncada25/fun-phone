package com.bookverse.contacts.domain.call

import android.graphics.Bitmap
import android.net.Uri

data class CallContactPresentation(
    val name: String,
    val photoUri: Uri?,
    val incomingCallBackgroundUri: Uri? = null,
    val inCallBackgroundUri: Uri? = null,
    val phones: List<CallPhonePresentation>,
)

data class CallPhonePresentation(
    val number: String,
    val type: CallPhoneType,
)

enum class CallPhoneType {
    MOBILE,
    WORK,
    HOME,
    MAIN,
    OTHER,
}

interface CallContactLookup {
    suspend fun findByPhoneNumber(phoneNumber: String): CallContactPresentation?
    suspend fun loadPhoto(uri: Uri): Bitmap?
}
