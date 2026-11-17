package com.bookverse.contacts.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import com.bookverse.contacts.R
import java.util.UUID

/** Clears only the clip created by this action, never a later copy from another app. */
object SensitiveClipboard {
    private const val OWNER = "com.bookverse.contacts.clip_owner"
    fun copy(context: Context, value: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val owner = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(context.getString(R.string.password_copy_action), value)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ClipDescription.EXTRA_IS_SENSITIVE
                else "android.content.extra.IS_SENSITIVE",
                true,
            )
            putString(OWNER, owner)
        }
        clipboard.setPrimaryClip(clip)
        Handler(Looper.getMainLooper()).postDelayed({
            // Background clipboard access may be denied by Android. Never request broader access.
            runCatching {
                if (clipboard.primaryClipDescription?.extras?.getString(OWNER) == owner) clipboard.clearPrimaryClip()
            }
        }, 30_000)
    }
}
