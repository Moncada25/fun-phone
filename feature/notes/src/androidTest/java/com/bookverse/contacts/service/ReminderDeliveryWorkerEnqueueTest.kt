package com.bookverse.contacts.service

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDeliveryWorkerEnqueueTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val noteId = 9_876_543L
    private val notificationType = "alarm"
    private val uniqueWorkName =
        ReminderDeliveryWorker.uniqueWorkName(noteId, notificationType)

    @After
    fun cleanUp() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(uniqueWorkName)
            .result
            .get(10, TimeUnit.SECONDS)
    }

    @Test
    fun shouldEnqueueDurableWork() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("notificationType", notificationType)
        }

        ReminderDeliveryWorker.enqueue(context, intent)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(uniqueWorkName)
            .get(10, TimeUnit.SECONDS)
        assertTrue(workInfos.isNotEmpty())
    }
}
