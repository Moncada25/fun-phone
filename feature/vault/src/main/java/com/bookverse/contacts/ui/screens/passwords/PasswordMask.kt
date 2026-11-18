package com.bookverse.contacts.ui.screens.passwords

internal const val PASSWORD_MASK_CHARACTER = '●'

internal fun maskPasswordValue(value: String): String =
    PASSWORD_MASK_CHARACTER.toString().repeat(value.length)
