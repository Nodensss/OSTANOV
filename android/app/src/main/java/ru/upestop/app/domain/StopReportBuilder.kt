package ru.upestop.app.domain

import ru.upestop.app.data.model.CheckItem
import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm
import java.time.ZoneId
import java.util.Locale

/** Шаблоны строк журнала. В приложении берутся из strings.xml, в тестах задаются явно. */
data class ReportStrings(
    val header: String,
    val generated: String,
    val done: String,
    val marksRange: String,
    val noMarks: String,
    val timersHeader: String,
    val timerLine: String,
    val timerIdle: String,
    val timerRunning: String,
    val timerPaused: String,
    val timerFinished: String,
    val phaseLine: String,
    val critical: String,
)

/** Текст журнала остановки (формат — Приложение В ТЗ). */
object StopReportBuilder {

    fun build(
        algorithm: StopAlgorithm,
        checked: Map<String, Long>,
        timers: Map<Int, TimerState>,
        now: Long,
        zone: ZoneId,
        strings: ReportStrings,
    ): String = buildString {
        val total = algorithm.totalChecks
        val done = Progress.completed(algorithm, checked)
        fun stamp(epochMs: Long) = TimeFormat.smart(epochMs, now, zone)

        appendLine(strings.header)
        appendLine(algorithm.title)
        if (algorithm.subtitle.isNotBlank()) appendLine(algorithm.subtitle)
        appendLine(strings.generated.format(TimeFormat.dateTime(now, zone)))
        appendLine(strings.done.format(done, total, Progress.percent(done, total)))
        val range = Progress.markRange(algorithm, checked)
        if (range == null) {
            appendLine(strings.noMarks)
        } else {
            appendLine(strings.marksRange.format(TimeFormat.dateTime(range.first, zone), TimeFormat.dateTime(range.second, zone)))
        }

        val timerPhases = algorithm.phases.filter { it.timer != null }
        if (timerPhases.isNotEmpty()) {
            appendLine()
            appendLine(strings.timersHeader)
            timerPhases.forEach { phase ->
                val spec = phase.timer ?: return@forEach
                val state = timers[phase.id] ?: TimerState(phase.id)
                val duration = state.durationMs ?: TimerMath.durationMs(spec.minutes, testMode = false)
                val status = when {
                    state.finished -> strings.timerFinished.format(
                        state.firstStartedAt?.let(::stamp).orEmpty(),
                        state.finishedAt?.let(::stamp).orEmpty(),
                    )
                    state.running -> strings.timerRunning.format(
                        state.firstStartedAt?.let(::stamp).orEmpty(),
                        TimerMath.formatElapsed(TimerMath.elapsed(state, duration, now)),
                    )
                    state.paused -> strings.timerPaused.format(TimerMath.formatElapsed(state.elapsedMs))
                    else -> strings.timerIdle
                }
                appendLine(strings.timerLine.format(phase.id, spec.label, spec.minutes, status))
            }
        }

        algorithm.sections().forEach { section ->
            appendLine()
            appendLine("== $section ==")
            algorithm.phases.filter { it.section == section }.forEach { phase ->
                appendLine(phaseLine(phase, checked, strings))
                phase.checks.forEach { check -> appendLine(checkLine(check, checked[check.id], ::stamp)) }
            }
        }
    }.trimEnd() + "\n"

    private fun phaseLine(phase: Phase, checked: Map<String, Long>, strings: ReportStrings): String = buildString {
        append(strings.phaseLine.format(phase.id, phase.title))
        if (phase.critical) append(" ").append(strings.critical)
        append(" [").append(Progress.phaseCompleted(phase, checked)).append('/').append(phase.checks.size).append(']')
        if (Progress.isPhaseDone(phase, checked)) append(" ✓")
        phase.condition?.takeIf { it.isNotBlank() }?.let { append(" (").append(it).append(')') }
    }

    private fun checkLine(check: CheckItem, checkedAt: Long?, stamp: (Long) -> String): String = buildString {
        append(if (checkedAt != null) "  [x] " else "  [ ] ")
        append(check.id).append(' ').append(check.text)
        check.param?.takeIf { it.isNotBlank() }?.let { append(" [").append(it).append(']') }
        if (checkedAt != null) append(" — ").append(stamp(checkedAt))
    }

    private fun String.format(vararg args: Any): String = String.format(Locale.ROOT, this, *args)
}
