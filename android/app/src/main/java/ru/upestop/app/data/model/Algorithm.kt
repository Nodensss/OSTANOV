package ru.upestop.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AlgorithmJson = Json { ignoreUnknownKeys = true }

@Serializable
data class StopAlgorithm(val title: String, val subtitle: String, val sources: List<String>,
    val phases: List<Phase>, val criticalRules: List<CriticalRule>, val trainerQuestions: List<TrainerQuestion>)
@Serializable
data class Phase(val id: Int, val section: String, val title: String, val subtitle: String,
    val condition: String? = null, val critical: Boolean = false, val checks: List<CheckItem>,
    val warnings: List<WarningItem>? = null, val positions: List<String>, val timer: TimerSpec? = null,
    val scheme: Scheme? = null)
@Serializable
enum class CheckType { @SerialName("check") CHECK, @SerialName("param") PARAM }
@Serializable
data class CheckItem(val id: String, val text: String, val type: CheckType, val param: String? = null)
@Serializable
data class WarningItem(val title: String, val text: String)
@Serializable
data class TimerSpec(val minutes: Int, val label: String)
@Serializable
data class Scheme(val title: String, val lines: List<String>)
@Serializable
data class CriticalRule(val id: String, val title: String, val rule: String, val consequence: String)
@Serializable
data class TrainerQuestion(val id: String, val phaseId: Int? = null, val question: String,
    val options: List<String>, val correct: Int, val explanation: String)
