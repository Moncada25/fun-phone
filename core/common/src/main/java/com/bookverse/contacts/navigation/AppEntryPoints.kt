package com.bookverse.contacts.navigation

import android.content.Context
import android.content.Intent

object AppEntryPoints {
    const val MAIN_ACTIVITY = "com.bookverse.contacts.MainActivity"

    fun mainActivityIntent(context: Context): Intent = Intent().setClassName(context, MAIN_ACTIVITY)
}
