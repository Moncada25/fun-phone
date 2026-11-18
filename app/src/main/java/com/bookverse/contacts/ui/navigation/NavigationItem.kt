package com.bookverse.contacts.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.bookverse.contacts.core.ui.R
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide

sealed class NavigationItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    data object Favorites : NavigationItem(
        route = "favorites",
        titleResId = R.string.tab_favorites,
        icon = Icons.Filled.Favorite
    )

    data object Contacts : NavigationItem(
        route = "contacts",
        titleResId = R.string.tab_contacts,
        icon = Icons.Filled.Contacts
    )

    data object Recents : NavigationItem(
        route = "history",
        titleResId = R.string.tab_history,
        icon = Lucide.History
    )

    data object Extras : NavigationItem(
        route = "extras",
        titleResId = R.string.tab_extras,
        icon = Icons.Filled.Star
    )
}
