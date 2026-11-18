package com.bookverse.contacts.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.core.ui.R

internal data class DynamicShortcutSpec(
    val destination: HomeShortcutDestination,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
)

object DynamicShortcuts {
    private const val ID_PREFIX = "dynamic_shortcut_"

    internal fun specs(vaultEnabled: Boolean): List<DynamicShortcutSpec> = listOfNotNull(
        DynamicShortcutSpec(HomeShortcutDestination.DIALER, R.string.dialer_shortcut_title, R.drawable.ic_shortcut_dialer),
        DynamicShortcutSpec(HomeShortcutDestination.NEW_NOTE, R.string.shortcut_new_note, R.drawable.ic_shortcut_note),
        DynamicShortcutSpec(HomeShortcutDestination.QR_SCANNER, R.string.qr_scanner_title, R.drawable.ic_shortcut_qr),
        DynamicShortcutSpec(HomeShortcutDestination.PASSWORDS, R.string.passwords_title, R.drawable.ic_shortcut_vault)
            .takeIf { vaultEnabled },
    )

    fun launchIntent(context: Context, destination: HomeShortcutDestination): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(HomeShortcutDestination.EXTRA_KEY, destination.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun publish(context: Context, vaultEnabled: Boolean) {
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val shortcuts = specs(vaultEnabled).take(limit).mapIndexed { index, spec ->
            val label = context.getString(spec.labelRes)
            ShortcutInfoCompat.Builder(context, ID_PREFIX + spec.destination.id)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(IconCompat.createWithResource(context, spec.iconRes))
                .setIntent(launchIntent(context, spec.destination))
                .setRank(index)
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
