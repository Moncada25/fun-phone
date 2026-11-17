package com.bookverse.contacts.ui.screens.tools

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal object HolidayDateFormatter {
    fun holiday(dateString: String, locale: Locale = Locale.ENGLISH): String = runCatching {
        LocalDate.parse(dateString).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale),
        )
    }.getOrDefault(dateString)

    fun eventDateTime(timestamp: Long, locale: Locale = Locale.ENGLISH): String =
        runCatching {
            val dateTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            dateTime.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale),
            )
        }.getOrDefault("")

    fun dateHeader(dateString: String, locale: Locale = Locale.ENGLISH): String = runCatching {
        LocalDate.parse(dateString)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(locale) else character.toString()
            }
    }.getOrDefault(dateString)

    fun eventTime(
        startDate: Long,
        endDate: Long,
        locale: Locale = Locale.ENGLISH,
    ): String = runCatching {
        val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        val start = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val end = Instant.ofEpochMilli(endDate)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        "${start.format(format)} - ${end.format(format)}"
    }.getOrDefault("")

    fun eventDate(
        timestamp: Long,
        allDay: Boolean,
        locale: Locale = Locale.ENGLISH,
    ): String = runCatching {
        val zone = if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
        Instant.ofEpochMilli(timestamp)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }.getOrDefault("")

    fun eventClockTime(timestamp: Long, locale: Locale = Locale.ENGLISH): String = runCatching {
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }.getOrDefault("")
}
