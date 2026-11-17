package com.bookverse.contacts.service

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
class ReminderSchedulingPerformanceTest {
    @Test
    fun schedulingCallerTimeStaysWithinBudget() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val noteIds = (0 until 20).map { 900_000L + it }
        val durationsMs = try {
            noteIds.map { noteId ->
                val note = Note(id = noteId, title = "Synthetic reminder")
                val reminder = ReminderData(
                    noteId = noteId,
                    date = System.currentTimeMillis() + 86_400_000L,
                    interval = 0,
                    message = "Synthetic",
                    type = ReminderType.SINGLE_EVENT,
                )
                val duration = measureNanoTime {
                    NotificationService.scheduleNotification(context, note, reminder)
                } / 1_000_000.0
                NotificationService.cancelNotification(context, noteId)
                duration
            }.sorted()
        } finally {
            noteIds.forEach { noteId ->
                NotificationService.cancelNotification(context, noteId)
            }
        }
        val percentile90 = durationsMs[(durationsMs.size * 9 / 10) - 1]
        println("PERF-05 p90_ms=$percentile90 samples=${durationsMs.size}")

        assertTrue(
            "PERF-05 exceeded 100 ms at p90: $percentile90 ms",
            percentile90 <= 100.0,
        )
    }
}
