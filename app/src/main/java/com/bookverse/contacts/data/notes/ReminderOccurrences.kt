package com.bookverse.contacts.data.notes

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val ReminderData.effectiveRepeat: ReminderRepeat
    get() = if (type == ReminderType.SINGLE_EVENT) repeatMode else ReminderRepeat.NONE

val ReminderData.repeats: Boolean
    get() = effectiveRepeat != ReminderRepeat.NONE

fun ReminderData.nextOccurrence(
    afterMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long? = nextReminderOccurrence(date, effectiveRepeat, afterMillis, zone)

fun nextReminderOccurrence(
    anchorMillis: Long,
    repeatMode: ReminderRepeat,
    afterMillis: Long,
    zone: ZoneId,
): Long? {
    if (anchorMillis > afterMillis) return anchorMillis
    val unit = when (repeatMode) {
        ReminderRepeat.NONE -> return null
        ReminderRepeat.DAILY -> ChronoUnit.DAYS
        ReminderRepeat.WEEKLY -> ChronoUnit.WEEKS
        ReminderRepeat.MONTHLY -> ChronoUnit.MONTHS
    }
    val anchorLocal = Instant.ofEpochMilli(anchorMillis).atZone(zone).toLocalDateTime()
    val afterLocal = Instant.ofEpochMilli(afterMillis).atZone(zone).toLocalDateTime()
    var step = (unit.between(anchorLocal, afterLocal) - 1L).coerceAtLeast(1L)
    for (attempt in 0 until MAX_ROLL_FORWARD_STEPS) {
        val candidate = reminderOccurrenceAt(anchorLocal, unit, step, zone)
        if (candidate > afterMillis) return candidate
        step++
    }
    return null
}

private fun reminderOccurrenceAt(
    anchorLocal: LocalDateTime,
    unit: ChronoUnit,
    step: Long,
    zone: ZoneId,
): Long = anchorLocal.plus(step, unit).atZone(zone).toInstant().toEpochMilli()

private const val MAX_ROLL_FORWARD_STEPS = 8
