package com.bookverse.contacts.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object AppDataStore {
    private const val DATA_STORE_NAME = "app_settings_datastore"
    private val Context.dataStoreInstance by preferencesDataStore(name = DATA_STORE_NAME)

    fun getDataStore(context: Context): DataStore<Preferences> {
        return context.dataStoreInstance
    }
}

interface AppSettingsStore {
    fun dataStore(context: Context): DataStore<Preferences>
}

object DataStoreAppSettingsStore : AppSettingsStore {
    override fun dataStore(context: Context): DataStore<Preferences> =
        AppDataStore.getDataStore(context.applicationContext)
}
