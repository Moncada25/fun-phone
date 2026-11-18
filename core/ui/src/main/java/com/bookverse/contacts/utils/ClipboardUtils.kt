package com.bookverse.contacts.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.bookverse.contacts.core.ui.R

object ClipboardUtils {
    fun copyToClipboard(
        context: Context,
        text: String,
        toast: Boolean = true,
        label: String? = null,
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label ?: context.getString(R.string.copied_to_clipboard), text)
        clipboard.setPrimaryClip(clip)

        if (toast) {
            Toast.makeText(
                context, context.getString(R.string.qr_text_copied), Toast.LENGTH_SHORT
            ).show()
        }
    }
}
