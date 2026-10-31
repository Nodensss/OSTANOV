package ru.upestop.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormat {
    private val time = DateTimeFormatter.ofPattern("HH:mm")
    private val dayTime = DateTimeFormatter.ofPattern("dd.MM HH:mm")
    private val dateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun time(epochMs: Long, zone: ZoneId): String = time.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun dateTime(epochMs: Long, zone: ZoneId): String =
        dateTime.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    /** HH:mm, если [epochMs] в тот же день, что [now], иначе dd.MM HH:mm. */
    fun smart(epochMs: Long, now: Long, zone: ZoneId): String {
        val moment = Instant.ofEpochMilli(epochMs).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return if (moment.toLocalDate() == today) time.format(moment) else dayTime.format(moment)
    }
}
