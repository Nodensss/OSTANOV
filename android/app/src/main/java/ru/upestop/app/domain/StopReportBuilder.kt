package ru.upestop.app.domain

import java.time.ZoneId
import ru.upestop.app.data.model.StopAlgorithm

// Labels are supplied by the Android resource adapter; the builder remains pure Kotlin.
fun interface ReportText { fun get(key: String, args: List<Any>): String }
class StopReportBuilder(private val text: ReportText) {
    private fun t(key: String, vararg args: Any) = text.get(key, args.toList())
    fun build(algorithm: StopAlgorithm, checks: List<CheckState>, timers: List<TimerState>, now: Long,
        zoneId: ZoneId, testMode: Boolean = false): String = buildString {
        val progress = Progress(algorithm, checks)
        appendLine(t("report_header")); appendLine(algorithm.title); appendLine(algorithm.subtitle)
        appendLine(t("report_generated", TimeFormat.format(now, zoneId, "dd.MM.yyyy HH:mm")))
        appendLine(t("report_progress", progress.count, progress.total, TrainerEngine.percent(progress.count, progress.total)))
        val times = progress.checked.values.mapNotNull { it.checkedAt }
        if (times.isNotEmpty()) appendLine(t("report_range", TimeFormat.format(times.min(), zoneId, "dd.MM.yyyy HH:mm"), TimeFormat.format(times.max(), zoneId, "dd.MM.yyyy HH:mm")))
        appendLine(); appendLine(t("report_timers"))
        algorithm.phases.filter { it.timer != null }.forEach { phase ->
            val timer = timers.find { it.phaseId == phase.id } ?: TimerState(phase.id)
            val spec = phase.timer!!
            val status = when {
                timer.finished -> t("report_timer_done", TimeFormat.format(timer.firstStartedAt ?: now, zoneId), TimeFormat.format(timer.finishedAt ?: now, zoneId))
                timer.running -> t("report_timer_running", TimerMath.format(TimerMath.elapsed(timer, TimerMath.duration(spec.minutes, testMode), now)))
                timer.firstStartedAt != null -> t("report_timer_paused", TimerMath.format(timer.elapsedMs))
                else -> t("report_timer_idle")
            }
            appendLine(t("report_timer", phase.id, spec.label, spec.minutes, status))
        }
        algorithm.phases.groupBy { it.section }.forEach { (section, phases) ->
            appendLine(); appendLine("== $section ==")
            phases.forEach { phase ->
                val count = phase.checks.count { it.id in progress.checked }
                append(t("report_phase", phase.id, phase.title))
                if (phase.critical) append(" ${t("report_critical")}")
                append(" [$count/${phase.checks.size}]")
                if (progress.phaseDone(phase)) append(" ✓")
                phase.condition?.let { append(" ($it)") }; appendLine()
                phase.checks.forEach { check ->
                    val mark = progress.checked[check.id]
                    append("  [${if (mark != null) "x" else " "}] ${check.id} ${check.text}")
                    check.param?.let { append(" [$it]") }
                    mark?.checkedAt?.let { append(" — ${TimeFormat.mark(it, now, zoneId)}") }
                    appendLine()
                }
            }
        }
    }
}
