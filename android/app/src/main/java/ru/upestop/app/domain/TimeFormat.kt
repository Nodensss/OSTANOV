package ru.upestop.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    fun format(time: Long, zoneId: ZoneId = ZoneId.systemDefault(), pattern: String = "HH:mm"): String =
        Instant.ofEpochMilli(time).atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag("ru")))
    fun mark(time: Long, now: Long, zoneId: ZoneId): String = format(time, zoneId,
        if (Instant.ofEpochMilli(time).atZone(zoneId).toLocalDate() == Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()) "HH:mm" else "dd.MM HH:mm")
}
