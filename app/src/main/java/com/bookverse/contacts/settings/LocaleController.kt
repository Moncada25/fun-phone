package com.bookverse.contacts.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.ui.viewmodel.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/** Owns the persisted and process-wide application locale. */
class LocaleController(
    private val settingsStore: AppSettingsStore = DataStoreAppSettingsStore,
) {
    suspend fun readAndMigrate(context: Context): AppLanguage {
        val dataStore = settingsStore.dataStore(context.applicationContext)
        val preferences = dataStore.data.first()
        val language = languageForStoredValue(preferences[appLanguageKey])
        val stableTag = stableLanguageTag(language)

        if (preferences[appLanguageKey] != stableTag) {
            dataStore.edit { it[appLanguageKey] = stableTag }
        }
        return language
    }

    suspend fun persist(context: Context, language: AppLanguage) {
        settingsStore.dataStore(context.applicationContext).edit {
            it[appLanguageKey] = stableLanguageTag(language)
        }
    }

    fun initializeAtStartup(context: Context) {
        val language = try {
            runBlocking(Dispatchers.IO) {
                readAndMigrate(context)
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.LOCALE_LOAD_FAILED)
            AppLanguage.SYSTEM
        }
        apply(language)
    }

    fun apply(language: AppLanguage) {
        val locales = localeListFor(language)
        appliedLocales = locales
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        const val APP_LANGUAGE_KEY_NAME = "app_language"
        const val SYSTEM_LANGUAGE_TAG = "system"

        private val appLanguageKey = stringPreferencesKey(APP_LANGUAGE_KEY_NAME)

        private val stableTags = mapOf(
            AppLanguage.SYSTEM to SYSTEM_LANGUAGE_TAG,
            AppLanguage.SPANISH to "es",
            AppLanguage.ENGLISH to "en",
            AppLanguage.PORTUGUESE to "pt",
            AppLanguage.FRENCH to "fr",
            AppLanguage.GERMAN to "de",
        )

        private val languagesByTag = stableTags.entries.associate { (language, tag) ->
            tag to language
        }

        @Volatile
        private var appliedLocales: LocaleListCompat = LocaleListCompat.getEmptyLocaleList()

        fun stableLanguageTag(language: AppLanguage): String =
            stableTags[language] ?: SYSTEM_LANGUAGE_TAG

        fun languageForStoredValue(value: String?): AppLanguage {
            val normalized = value?.trim()?.lowercase(java.util.Locale.ROOT)
            if (normalized.isNullOrEmpty() || normalized == SYSTEM_LANGUAGE_TAG) {
                return AppLanguage.SYSTEM
            }

            languagesByTag[normalized]?.let { return it }

            // Existing backups and DataStore values used enum names such as SPANISH.
            AppLanguage.entries.firstOrNull {
                it.name.lowercase(java.util.Locale.ROOT) == normalized
            }?.let { return it }

            // Accept a regional BCP-47 tag and normalize it to a supported base tag.
            val baseLanguage = java.util.Locale.forLanguageTag(normalized).language
                .lowercase(java.util.Locale.ROOT)
            return languagesByTag[baseLanguage] ?: AppLanguage.SYSTEM
        }

        fun localeListFor(language: AppLanguage): LocaleListCompat {
            val stableTag = stableLanguageTag(language)
            return if (stableTag == SYSTEM_LANGUAGE_TAG) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(stableTag)
            }
        }

        /** Applies the selected locale to activity contexts on pre-33 devices too. */
        fun localizedContext(baseContext: Context): Context {
            val locales = appliedLocales
            if (locales.isEmpty) return baseContext

            val configuration = Configuration(baseContext.resources.configuration)
            ConfigurationCompat.setLocales(configuration, locales)
            return baseContext.createConfigurationContext(configuration)
        }

        fun effectiveLocale(context: Context): Locale {
            return appliedLocales.get(0)
                ?: ConfigurationCompat.getLocales(context.resources.configuration).get(0)
                ?: Locale.ENGLISH
        }
    }
}
