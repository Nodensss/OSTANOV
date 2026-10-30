package ru.upestop.app

import java.time.Instant
import java.time.ZoneId
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.domain.*

class StopReportBuilderTest {
    @Test fun exportIncludesCountsMarksParametersAndTimers() {
        val data = realAlgorithm()
        val now = Instant.parse("2026-10-30T14:05:00Z").toEpochMilli()
        val check = data.phases.first().checks.first()
        val xml = File("src/main/res/values/strings.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml).getElementsByTagName("string")
        val strings = (0 until nodes.length).associate { i -> val node = nodes.item(i); node.attributes.getNamedItem("name").nodeValue to node.textContent }
        val builder = StopReportBuilder { key, args -> strings.getValue(key).format(java.util.Locale.ROOT, *args.toTypedArray()) }
        val report = builder.build(data, listOf(CheckState(check.id, true, now - 60000)), emptyList(), now, ZoneId.of("UTC"))
        assertTrue(report.startsWith("ЖУРНАЛ ОСТАНОВКИ\n${data.title}"))
        assertTrue(report.contains("Выполнено: 1 из ${data.phases.sumOf { it.checks.size }}"))
        assertTrue(report.contains("[x] ${check.id} ${check.text} — 14:04"))
        assertTrue(report.contains("[ ] ${data.phases.first().checks[1].id}"))
        assertTrue(report.contains("[${data.phases.flatMap { it.checks }.first { it.param != null }.param}]"))
        assertTrue(report.contains("ТАЙМЕРЫ"))
        assertTrue(report.contains("не запускался"))
        assertEquals("29.10 14:05", TimeFormat.mark(now - 86400000, now, ZoneId.of("UTC")))
        val timedPhase = data.phases.first { it.timer != null }
        val running = builder.build(data, emptyList(), listOf(TimerMath.start(TimerState(timedPhase.id), now - 60000)), now, ZoneId.of("UTC"))
        assertTrue(running.contains("идёт, прошло 01:00"))
    }
}
