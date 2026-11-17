package com.bookverse.contacts.ui.screens.contacts.dialer

import android.content.Context
import android.content.res.Resources
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.ui.components.AppSnackbarState
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.utils.ContactUtils

sealed interface SpeedDialResolution {
    data class Dial(val phoneNumber: String) : SpeedDialResolution
    data object ContactUnavailable : SpeedDialResolution
    data object NotConfigured : SpeedDialResolution
}

fun resolveSpeedDial(
    key: Int,
    assignments: Map<Int, String>,
    contacts: List<Contact>,
): SpeedDialResolution {
    val contactId = assignments[key] ?: return SpeedDialResolution.NotConfigured
    val phoneNumber = contacts
        .firstOrNull { it.id == contactId }
        ?.phones
        ?.firstOrNull()
        ?.number
        ?.takeIf { it.isNotBlank() }
        ?: return SpeedDialResolution.ContactUnavailable
    return SpeedDialResolution.Dial(phoneNumber)
}

fun handleSpeedDialLongPress(
    key: Int,
    assignments: Map<Int, String>,
    contacts: List<Contact>,
    context: Context,
    resources: Resources,
    snackbar: AppSnackbarState,
) {
    when (val resolution = resolveSpeedDial(key, assignments, contacts)) {
        is SpeedDialResolution.Dial ->
            ContactUtils.callPhoneNumber(context, resolution.phoneNumber)
        SpeedDialResolution.ContactUnavailable ->
            snackbar.show(
                resources.getString(R.string.speed_dial_contact_not_found),
                SnackbarType.Warning,
            )
        SpeedDialResolution.NotConfigured ->
            snackbar.show(
                resources.getString(R.string.speed_dial_not_configured, key),
                SnackbarType.Info,
            )
    }
}
