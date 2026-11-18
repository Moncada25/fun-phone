package com.bookverse.contacts.service

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderReceiverInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private var noteId = 0L

    @Before
    fun setUp() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        val noteDao = AppDatabase.getInstance(context).noteDao()
        noteId = noteDao.addNoteWithReminder(
            note = Note(
                title = "Receiver delivery test",
                isReminder = true,
            ),
            reminderData = ReminderData(
                noteId = 0L,
                date = System.currentTimeMillis() + 60_000L,
                interval = 0,
                message = EXPECTED_MESSAGE,
                type = ReminderType.SINGLE_EVENT,
            ),
        )
        NotificationService.createNotificationChannels(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            notificationManager.cancel(NotificationService.NOTIFICATION_ID_PREFIX + noteId.toInt())
            val noteDao = AppDatabase.getInstance(context).noteDao()
            noteDao.getNoteById(noteId)?.let { noteDao.deleteNote(it) }
        }
    }

    @Test
    fun receiverPublishesNotificationDirectly() {
        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderDeliveryCoordinator.KEY_NOTE_ID, noteId)
                putExtra(ReminderDeliveryCoordinator.KEY_NOTIFICATION_TYPE, "alarm")
            },
        )

        assertTrue(waitForNotification())
    }

    private fun waitForNotification(): Boolean {
        repeat(50) {
            val wasPublished = notificationManager.activeNotifications.any { activeNotification ->
                activeNotification.notification.extras
                    .getCharSequence(android.app.Notification.EXTRA_TEXT)
                    ?.toString() == EXPECTED_MESSAGE
            }
            if (wasPublished) return true
            Thread.sleep(100L)
        }
        return false
    }

    companion object {
        private const val EXPECTED_MESSAGE = "Published without WorkManager"
    }
}
