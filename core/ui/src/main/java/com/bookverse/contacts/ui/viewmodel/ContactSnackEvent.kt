package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.ui.components.SnackbarType

data class ContactSnackEvent(
    val message: String,
    val kind: SnackbarType = SnackbarType.Info,
    val actionLabel: String? = null,
)
