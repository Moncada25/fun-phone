package com.bookverse.contacts.ui.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.bookverse.contacts.settings.LocaleController

/** Shared activity boundary that keeps AppCompat and pre-33 resource locales aligned. */
abstract class LocalizedAppCompatActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.localizedContext(newBase))
    }
}
