package com.bookverse.contacts.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import contacts.core.entities.PhoneEntity

@Composable
fun getPhoneTypeLabel(type: PhoneEntity.Type?): String {
    return when (type) {
        PhoneEntity.Type.MOBILE -> stringResource(R.string.phone_type_mobile)
        PhoneEntity.Type.WORK -> stringResource(R.string.phone_type_work)
        PhoneEntity.Type.HOME -> stringResource(R.string.phone_type_home)
        PhoneEntity.Type.MAIN -> stringResource(R.string.phone_type_main)
        else -> stringResource(R.string.phone_type_mobile)
    }
}