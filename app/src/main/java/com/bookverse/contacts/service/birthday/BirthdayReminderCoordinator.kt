package com.bookverse.contacts.service.birthday

import android.content.Context
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.domain.birthday.BirthdayContact
import com.bookverse.contacts.domain.birthday.BirthdayReminderPolicy
import java.time.Clock
import java.time.ZoneId

fun interface BirthdayContactSource {
    suspend fun getContactsWithBirthdays(): List<BirthdayContact>
}

class CachedBirthdayContactSource(context: Context) : BirthdayContactSource {
    private val repository = ContactCacheRepository(context.applicationContext)

    override suspend fun getContactsWithBirthdays(): List<BirthdayContact> =
        repository.getCachedContacts().mapNotNull { contact ->
            contact.birthday?.let { birthday ->
                BirthdayContact(
                    id = contact.id,
                    name = contact.name,
                    birthdayMillis = birthday,
                )
            }
        }
}

class BirthdayReminderCoordinator(
    context: Context,
    private val contactSource: BirthdayContactSource = CachedBirthdayContactSource(context),
    private val notifier: BirthdayReminderNotifier = BirthdayReminderNotifier(context),
    private val deliveryStore: BirthdayReminderDeliveryStore = BirthdayReminderDeliveryStore(context),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun deliver() {
        val now = clock.instant()
        val deliveryDate = now.atZone(zoneId).toLocalDate()
        val reminders = BirthdayReminderPolicy.remindersFor(
            contacts = contactSource.getContactsWithBirthdays(),
            now = now,
            zoneId = zoneId,
        ).filterNot { reminder ->
            deliveryStore.wasDelivered(deliveryDate, reminder.contact.id)
        }

        if (reminders.isEmpty()) return

        if (notifier.notify(reminders)) {
            deliveryStore.markDelivered(deliveryDate, reminders.map { it.contact.id })
        }
    }
}
