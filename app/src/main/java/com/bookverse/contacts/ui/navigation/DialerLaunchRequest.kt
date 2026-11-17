package com.bookverse.contacts.ui.navigation

import android.content.Intent

data class DialerLaunchRequest(val number: String)

/** Resolves a prefill request only; an external intent never places a call. */
fun resolveDialerLaunchRequest(action: String?, scheme: String?, decodedNumber: String?): DialerLaunchRequest? {
    if (action == Intent.ACTION_CALL_BUTTON) return DialerLaunchRequest("")
    if (action == Intent.ACTION_DIAL && scheme == null) return DialerLaunchRequest("")
    if (action !in listOf(Intent.ACTION_DIAL, Intent.ACTION_VIEW, Intent.ACTION_CALL) || !scheme.equals("tel", ignoreCase = true)) return null
    return DialerLaunchRequest(decodedNumber.orEmpty().filter { it in '0'..'9' || it in "+*#" })
}
