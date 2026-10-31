package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StopReportBuilderTest {
    private val zone = ZoneId.of("Asia/Tomsk")
    private val algorithm = TestData.sample()
    private fun at(day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private val strings = ReportStrings(
        header = "ЖУРНАЛ ОСТАНОВКИ",
        generated = "Сформирован: %1\$s",
        done = "Выполнено: %1\$d из %2\$d (%3\$d%%)",
        marksRange = "Первая отметка: %1\$s · Последняя: %2\$s",
        noMarks = "Отметок пока нет",
        timersHeader = "ТАЙМЕРЫ",
        timerLine = "Этап %1\$d · %2\$s (%3\$d мин): %4\$s",
        timerIdle = "не запускался",
        timerRunning = "запущен %1\$s, идёт, прошло %2\$s",
        timerPaused = "на паузе, прошло %1\$s",
        timerFinished = "запущен %1\$s, завершён %2\$s",
        phaseLine = "Этап %1\$d · %2\$s",
        critical = "[КРИТИЧНО]",
    )

    private fun build(checked: Map<String, Long>, timers: Map<Int, TimerState> = emptyMap(), now: Long = at(30, 14, 5)) =
        StopReportBuilder.build(algorithm, checked, timers, now, zone, strings)

    @Test
    fun headerAndTotals() {
        val lines = build(mapOf("1.1" to at(30, 8, 12), "1.2" to at(30, 8, 15))).lines()
        assertEquals("ЖУРНАЛ ОСТАНОВКИ", lines[0])
        assertEquals("Тестовый алгоритм", lines[1])
        assertEquals("Подзаголовок", lines[2])
        assertEquals("Сформирован: 30.10.2026 14:05", lines[3])
        assertEquals("Выполнено: 2 из 5 (40%)", lines[4])
        assertEquals("Первая отметка: 30.10.2026 08:12 · Последняя: 30.10.2026 08:15", lines[5])
    }

    @Test
    fun noMarks() {
        assertTrue(build(emptyMap()).lines().contains("Отметок пока нет"))
    }

    @Test
    fun phaseAndCheckLines() {
        val report = build(mapOf("1.1" to at(30, 8, 12), "1.2" to at(29, 23, 50)))
        val lines = report.lines()
        assertTrue(lines.contains("== Раздел А =="))
        assertTrue(lines.contains("Этап 1 · Первый этап [2/2] ✓"))
        assertTrue(lines.contains("  [x] 1.1 Открыть А-401 — 08:12"))
        // Отметка не в день формирования журнала — с датой; параметр в квадратных скобках.
        assertTrue(lines.contains("  [x] 1.2 Давление [500 кгс/см²] — 29.10 23:50"))
        assertTrue(lines.contains("Этап 2 · Второй этап [КРИТИЧНО] [0/1] (Только при длительной остановке)"))
        assertTrue(lines.contains("  [ ] 2.1 Закрыть 14047"))
        assertTrue(report.endsWith("\n"))
    }

    @Test
    fun timerStatuses() {
        val minute = TimerMath.MINUTE_MS
        val duration = 40 * minute
        val idle = build(emptyMap())
        assertTrue(idle.lines().contains("Этап 2 · Циркуляция (40 мин): не запускался"))

        val started = TimerMath.start(TimerState(2), duration, at(30, 11, 2))
        val finished = TimerMath.finish(started, duration, at(30, 11, 42))
        assertTrue(build(emptyMap(), mapOf(2 to finished)).lines().contains("Этап 2 · Циркуляция (40 мин): запущен 11:02, завершён 11:42"))

        val running = build(emptyMap(), mapOf(2 to started), now = at(30, 11, 14) + 30_000)
        assertTrue(running.lines().contains("Этап 2 · Циркуляция (40 мин): запущен 11:02, идёт, прошло 0:12:30"))

        val paused = TimerMath.pause(started, at(30, 11, 47))
        assertTrue(build(emptyMap(), mapOf(2 to paused)).lines().contains("Этап 2 · Циркуляция (40 мин): на паузе, прошло 0:40:00"))
    }
}
