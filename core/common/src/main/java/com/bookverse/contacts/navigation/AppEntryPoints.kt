package com.bookverse.contacts.navigation

import android.content.Context
import android.content.Intent

object AppEntryPoints {
    const val MAIN_ACTIVITY = "com.bookverse.contacts.MainActivity"
    const val CONTACT_DETAIL_ACTIVITY = "com.bookverse.contacts.widget.ContactDetailActivity"
    const val EXTRA_CONTACT_ID = "contact_id"
    const val EXTRA_CONTACT_NAME = "contact_name"
    const val EXTRA_CONTACT_PHONE = "contact_phone"

    fun mainActivityIntent(context: Context): Intent = Intent().setClassName(context, MAIN_ACTIVITY)

    fun contactDetailIntent(context: Context): Intent = Intent().setClassName(context, CONTACT_DETAIL_ACTIVITY)
}
