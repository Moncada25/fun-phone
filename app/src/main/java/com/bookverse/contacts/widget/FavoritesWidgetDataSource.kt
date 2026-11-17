package com.bookverse.contacts.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.utils.initialInfo
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.FavoritesSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale

object FavoritesWidgetDataSource {

    private val favoritesSortKey =
        stringPreferencesKey(AppSettingsViewModel.FAVORITES_SORT_KEY_NAME)
    private val favoritesCustomOrderKey =
        stringPreferencesKey(AppSettingsViewModel.FAVORITES_CUSTOM_ORDER_KEY_NAME)

    suspend fun loadFavorites(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        val repository = ContactRepository(context)
        val favorites = runCatching { repository.getContacts() }
            .getOrElse { emptyList() }
            .filter { it.isFavorite }

        if (favorites.isEmpty()) {
            return@withContext emptyList()
        }

        val alphabetical = favorites.sortedWith(
            compareBy<Contact> { it.initialInfo().category }
                .thenBy { it.initialInfo().sortKey }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        val prefs = runCatching {
            AppDataStore.getDataStore(context).data.first()
        }.getOrNull() ?: return@withContext alphabetical

        val sortOrder = FavoritesSortOrder.fromName(prefs[favoritesSortKey])
        if (sortOrder != FavoritesSortOrder.CUSTOM) {
            return@withContext alphabetical
        }

        val customOrderRaw = prefs[favoritesCustomOrderKey] ?: return@withContext alphabetical
        val normalizedOrder = customOrderRaw.split("|")
            .mapNotNull { id -> id.takeIf { it.isNotBlank() } }
            .distinct()
        if (normalizedOrder.isEmpty()) {
            return@withContext alphabetical
        }

        val favoriteIds = alphabetical.map { it.id }
        val sanitizedOrder = normalizedOrder.filter { it in favoriteIds } +
            favoriteIds.filter { it !in normalizedOrder }
        if (sanitizedOrder.isEmpty()) {
            return@withContext alphabetical
        }

        val orderMap = sanitizedOrder.withIndex().associate { it.value to it.index }
        val alphabeticalIndex = alphabetical.withIndex().associate { it.value.id to it.index }

        alphabetical.sortedWith(
            compareBy<Contact> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { alphabeticalIndex[it.id] ?: Int.MAX_VALUE }
        )
    }
}
