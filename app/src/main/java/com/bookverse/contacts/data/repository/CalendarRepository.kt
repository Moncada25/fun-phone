package com.bookverse.contacts.data.repository

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.bookverse.contacts.data.model.CalendarAttendee
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.domain.holidays.CalendarEventSource
import com.bookverse.contacts.domain.holidays.CalendarSource
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val context: Context) : CalendarEventSource {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    override suspend fun getCalendarEvents(
        calendarIds: Set<Long>,
        startDate: Long,
        endDate: Long,
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CALENDAR) || calendarIds.isEmpty()) {
            return@withContext emptyList()
        }

        val calendarMap = getCalendarsByIds(calendarIds).associateBy(CalendarSource::id)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DURATION,
        )
        val selection = buildString {
            append("${CalendarContract.Events.CALENDAR_ID} IN (")
            append(calendarIds.joinToString(",") { "?" })
            append(") AND ${CalendarContract.Events.DELETED} = 0")
        }
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startDate)
        ContentUris.appendId(uriBuilder, endDate)

        contentResolver.query(
            uriBuilder.build(),
            projection,
            selection,
            calendarIds.map(Long::toString).toTypedArray(),
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val recurrenceIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            val durationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DURATION)

            buildList {
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(idIndex).toString()
                    val calendarId = cursor.getLong(calendarIdIndex)
                    val calendar = calendarMap[calendarId]
                    add(
                        CalendarEvent(
                            id = eventId,
                            title = cursor.getString(titleIndex).orEmpty(),
                            description = cursor.getString(descriptionIndex),
                            location = cursor.getString(locationIndex),
                            startDate = cursor.getLong(startIndex),
                            endDate = cursor.getLong(endIndex),
                            calendarId = calendarId.toString(),
                            calendarName = calendar?.name.orEmpty(),
                            accountName = calendar?.accountName.orEmpty(),
                            allDay = cursor.getInt(allDayIndex) == 1,
                            attendees = getEventAttendees(eventId),
                            recurrenceRule = cursor.getString(recurrenceIndex),
                            duration = cursor.getString(durationIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    override suspend fun getCalendars(): List<CalendarSource> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CALENDAR)) return@withContext emptyList()
        queryCalendars(
            selection = "${CalendarContract.Calendars.VISIBLE} = 1",
            selectionArgs = null,
        ).distinctBy {
            Triple(it.name.trim(), it.accountName.trim(), it.accountType.trim())
        }
            .sortedWith(
                compareBy<CalendarSource> {
                    it.accountName.lowercase(LocaleController.effectiveLocale(context))
                }.thenBy {
                    it.name.lowercase(LocaleController.effectiveLocale(context))
                },
            )
    }

    private fun getCalendarsByIds(calendarIds: Set<Long>): List<CalendarSource> {
        if (calendarIds.isEmpty()) return emptyList()
        return queryCalendars(
            selection = buildString {
                append("${CalendarContract.Calendars._ID} IN (")
                append(calendarIds.joinToString(",") { "?" })
                append(") AND ${CalendarContract.Calendars.VISIBLE} = 1")
            },
            selectionArgs = calendarIds.map(Long::toString).toTypedArray(),
        )
    }

    private fun queryCalendars(
        selection: String,
        selectionArgs: Array<String>?,
    ): List<CalendarSource> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        return contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            )
            val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val typeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val primaryIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
            buildList {
                while (cursor.moveToNext()) {
                    val accountType = cursor.getString(typeIndex).orEmpty()
                    add(
                        CalendarSource(
                            id = cursor.getLong(idIndex),
                            name = cursor.getString(nameIndex).orEmpty(),
                            accountName = cursor.getString(accountIndex).orEmpty(),
                            accountType = accountType,
                            isPrimary = cursor.getInt(primaryIndex) == 1,
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun getEventAttendees(eventId: String): List<CalendarAttendee> {
        val projection = arrayOf(
            CalendarContract.Attendees.ATTENDEE_NAME,
            CalendarContract.Attendees.ATTENDEE_EMAIL,
            CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
            CalendarContract.Attendees.ATTENDEE_STATUS,
        )
        return contentResolver.query(
            CalendarContract.Attendees.CONTENT_URI,
            projection,
            "${CalendarContract.Attendees.EVENT_ID} = ?",
            arrayOf(eventId),
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_NAME)
            val emailIndex = cursor.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_EMAIL)
            val relationshipIndex = cursor.getColumnIndexOrThrow(
                CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
            )
            val statusIndex = cursor.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_STATUS)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarAttendee(
                            name = cursor.getString(nameIndex),
                            email = cursor.getString(emailIndex),
                            relationship = cursor.getInt(relationshipIndex),
                            status = cursor.getInt(statusIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

}
