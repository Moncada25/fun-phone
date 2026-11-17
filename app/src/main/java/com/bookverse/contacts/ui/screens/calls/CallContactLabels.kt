package com.bookverse.contacts.ui.screens.calls

import androidx.annotation.StringRes
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.call.CallPhoneType

@StringRes
internal fun callPhoneTypeLabel(type: CallPhoneType): Int = when (type) {
    CallPhoneType.MOBILE -> R.string.phone_type_mobile
    CallPhoneType.WORK -> R.string.phone_type_work
    CallPhoneType.HOME -> R.string.phone_type_home
    CallPhoneType.MAIN -> R.string.phone_type_main
    CallPhoneType.OTHER -> R.string.phone_type_mobile
}
