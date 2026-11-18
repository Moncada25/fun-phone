package com.bookverse.contacts.service.birthday

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bookverse.contacts.domain.birthday.BirthdayReminderPolicy
import java.time.Clock
import java.time.ZoneId

enum class BirthdayScheduleResult {
    SCHEDULED,
    EXACT_ALARM_PERMISSION_REQUIRED,
    FAILED,
}

class BirthdayReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNext(catchUpToday: Boolean = false): BirthdayScheduleResult {
        if (!canScheduleExactAlarms()) {
            cancel()
            return BirthdayScheduleResult.EXACT_ALARM_PERMISSION_REQUIRED
        }

        val now = clock.instant()
        val shouldCatchUp = catchUpToday && BirthdayReminderPolicy.shouldCatchUpToday(now, zoneId)
        val triggerAt = if (shouldCatchUp) {
            now.plusMillis(CATCH_UP_DELAY_MILLIS)
        } else {
            BirthdayReminderPolicy.nextNineAm(now, zoneId)
        }

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilli(),
                pendingIntent(),
            )
            BirthdayScheduleResult.SCHEDULED
        } catch (_: SecurityException) {
            BirthdayScheduleResult.EXACT_ALARM_PERMISSION_REQUIRED
        } catch (_: Exception) {
            BirthdayScheduleResult.FAILED
        }
    }

    fun cancel() {
        try {
            alarmManager.cancel(pendingIntent())
        } catch (_: Exception) {
            /* noop */
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_CODE,
        Intent(appContext, BirthdayReminderReceiver::class.java).setAction(ACTION_DELIVER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_DELIVER = "com.bookverse.contacts.action.DELIVER_BIRTHDAY_REMINDERS"
        private const val REQUEST_CODE = 9_001
        private const val CATCH_UP_DELAY_MILLIS = 1_000L
    }
}
