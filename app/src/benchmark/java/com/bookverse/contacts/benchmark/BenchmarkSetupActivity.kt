package com.bookverse.contacts.benchmark

import android.content.Intent
import android.os.Bundle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.ui.navigation.NavigationItem
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.repository.ContactCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BenchmarkSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contactCount = intent.getIntExtra(EXTRA_CONTACT_COUNT, DEFAULT_CONTACT_COUNT)
            .coerceIn(MIN_CONTACT_COUNT, MAX_CONTACT_COUNT)
        val includeAccountFixtures = intent.getBooleanExtra("include_account_fixtures", false)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ContactCacheRepository(applicationContext).replaceCachedContacts(
                    contacts = syntheticContacts(contactCount, includeAccountFixtures),
                    profile = null,
                )
                // The collector measures the 2,000-row contact list, not the 12-item favorites grid.
                AppDataStore.getDataStore(applicationContext).edit {
                    it[stringPreferencesKey("startup_screen")] = NavigationItem.Contacts.route
                }
                getSharedPreferences("onboarding", MODE_PRIVATE)
                    .edit()
                    .putBoolean("completed", true)
                    .putBoolean("limited_mode", true)
                    .commit()
            }
            startActivity(
                Intent(this@BenchmarkSetupActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            finish()
        }
    }

    private fun syntheticContacts(count: Int, includeAccountFixtures: Boolean): List<Contact> = List(count) { index ->
        val sequence = index + 1
        Contact(
            id = "benchmark-$sequence",
            name = "Benchmark Contact ${sequence.toString().padStart(4, '0')}",
            phones = listOf(
                ContactPhone(
                    number = "+1555${sequence.toString().padStart(7, '0')}",
                    type = null,
                ),
            ),
            isFavorite = sequence <= FAVORITE_COUNT,
            accounts = if (includeAccountFixtures) {
                when (sequence) {
                    2 -> emptyList()
                    3 -> listOf(ContactAccount.LOCAL_ACCOUNT)
                    4 -> listOf(
                        ContactAccount.LOCAL_ACCOUNT,
                        ContactAccount("QA account", "com.example.synthetic"),
                        ContactAccount.LOCAL_ACCOUNT,
                        ContactAccount("WhatsApp", ContactAccount.WHATSAPP),
                    )
                    else -> null
                }
            } else null,
        )
    }

    private companion object {
        const val EXTRA_CONTACT_COUNT = "contact_count"
        const val DEFAULT_CONTACT_COUNT = 2_000
        const val MIN_CONTACT_COUNT = 100
        const val MAX_CONTACT_COUNT = 10_000
        const val FAVORITE_COUNT = 12
    }
}
