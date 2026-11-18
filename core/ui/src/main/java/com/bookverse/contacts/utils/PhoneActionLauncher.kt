package com.bookverse.contacts.utils

import android.content.Context

interface PhoneActionLauncher {
    fun callPhoneNumber(context: Context, phoneNumber: String)

    fun openSmsApp(context: Context, phoneNumber: String)

    fun openWhatsApp(context: Context, phoneNumber: String)

    fun callVoicemail(context: Context)
}
