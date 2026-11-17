package com.bookverse.contacts.service.birthday

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

class BirthdayReminderDeliveryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun wasDelivered(date: LocalDate, contactId: String): Boolean = try {
        val deliveredIds = if (preferences.getString(KEY_DATE, null) == date.toString()) {
            preferences.getStringSet(KEY_CONTACT_IDS, emptySet()).orEmpty()
        } else {
            emptySet()
        }
        contactId in deliveredIds
    } catch (_: Exception) {
        false
    }

    fun markDelivered(date: LocalDate, contactIds: List<String>) {
        try {
            val existingIds = if (preferences.getString(KEY_DATE, null) == date.toString()) {
                preferences.getStringSet(KEY_CONTACT_IDS, emptySet()).orEmpty()
            } else {
                emptySet()
            }
            preferences.edit {
                putString(KEY_DATE, date.toString())
                putStringSet(KEY_CONTACT_IDS, existingIds + contactIds)
            }
        } catch (_: Exception) {
            /* noop */
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "birthday_reminder_delivery"
        private const val KEY_DATE = "delivery_date"
        private const val KEY_CONTACT_IDS = "contact_ids"
    }
}
