package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination

class NewNoteTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = LocaleController.localizedContext(this).getString(R.string.shortcut_new_note)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openNewNote) else openNewNote()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openNewNote() {
        val intent = DynamicShortcuts.launchIntent(this, HomeShortcutDestination.NEW_NOTE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
