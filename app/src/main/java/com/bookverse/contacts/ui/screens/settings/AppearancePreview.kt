package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AppearanceSettings
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.components.AppBarSearchAction
import com.bookverse.contacts.ui.components.AppBarSearchEntry
import com.bookverse.contacts.ui.components.DialerFloatingActionButton
import com.bookverse.contacts.ui.components.FloatingIconPill
import com.bookverse.contacts.ui.components.FloatingPill
import com.bookverse.contacts.ui.navigation.MainTabNavigationBar
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

@Composable
internal fun AppearancePreview(draft: AppearanceSettings, appliedFontScale: Float) {
    val description = stringResource(R.string.appearance_preview_description)
    ContactsAppTheme(
        accentSeed = draft.accent,
        accentBarStyle = draft.style,
        fontFamily = Fonts.getFontFamily(draft.font),
        fontScale = draft.fontScale / appliedFontScale.coerceAtLeast(0.8f),
        themeMode = draft.theme,
        darkModeStyle = draft.darkStyle,
    ) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(scheme.background).clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val retro = draft.style == AccentBarStyle.RETRO
            val minimal = draft.style == AccentBarStyle.MINIMAL
            Row(
                Modifier.fillMaxWidth()
                    .background(if (retro) MaterialTheme.accentPalette.seed else scheme.background)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val title: @Composable () -> Unit = {
                    Text(
                        stringResource(R.string.tab_contacts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (retro) MaterialTheme.accentPalette.onSeed else scheme.onSurface,
                    )
                }
                if (minimal) {
                    FloatingPill(backgroundColor = scheme.surface, contentColor = scheme.onSurface, content = title)
                } else {
                    title()
                }
                Spacer(Modifier.weight(1f))
                when {
                    minimal -> FloatingIconPill(onClick = {}, enabled = false, backgroundColor = scheme.surface, contentColor = scheme.onSurface) {
                        Icon(Lucide.Search, null, modifier = Modifier.size(20.dp))
                    }
                    retro -> Icon(Lucide.Search, null, tint = MaterialTheme.accentPalette.onSeed)
                }
            }
            if (draft.style == AccentBarStyle.MODERN) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    AppBarSearchEntry(AppBarSearchAction(stringResource(R.string.search_contacts), {}))
                }
            }
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = scheme.surface,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = draft.density.itemPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(draft.density.avatarSize).clip(CircleShape).background(scheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person, null, tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(draft.density.avatarSize * 0.6f),
                        )
                    }
                    Text(stringResource(R.string.appearance_preview_contact), style = MaterialTheme.typography.bodyLarge)
                }
            }
            DialerFloatingActionButton(
                style = draft.style,
                onClick = {},
                modifier = Modifier.align(Alignment.End).padding(end = 16.dp),
            )
            MainTabNavigationBar(
                tabs = listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras),
                indicatorProgress = 1f, backgroundColor = scheme.background, onTabSelected = {},
            )
        }
    }
}
