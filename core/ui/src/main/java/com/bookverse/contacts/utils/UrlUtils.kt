package com.bookverse.contacts.utils

import android.content.Context
import android.widget.Toast
import com.bookverse.contacts.core.ui.R

object UrlUtils {
    fun openUrl(context: Context, url: String): ExternalUriLauncher.Result {
        val result = ExternalUriLauncher.open(context, url)
        if (result != ExternalUriLauncher.Result.OPENED) {
            Toast.makeText(
                context,
                context.getString(R.string.url_open_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
        return result
    }
}
