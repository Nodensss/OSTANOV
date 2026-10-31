package ru.upestop.app.domain

import ru.upestop.app.data.model.AlgorithmJson
import ru.upestop.app.data.model.CheckItem
import ru.upestop.app.data.model.CheckType
import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.data.model.TimerSpec
import ru.upestop.app.data.model.TrainerQuestion
import java.io.File

object TestData {
    /** Настоящий data/stop-algorithm.json: путь передаёт Gradle (build.gradle.kts, systemProperty). */
    val repositoryJson: String by lazy {
        val path = requireNotNull(System.getProperty("algorithm.json")) { "system property algorithm.json не задано" }
        File(path).readText()
    }

    val repositoryAlgorithm: StopAlgorithm by lazy { AlgorithmJson.parse(repositoryJson) }

    /** Маленький синтетический алгоритм для точных проверок. */
    fun sample(): StopAlgorithm = StopAlgorithm(
        title = "Тестовый алгоритм",
        subtitle = "Подзаголовок",
        sources = listOf("Источник"),
        phases = listOf(
            Phase(
                id = 1,
                section = "Раздел А",
                title = "Первый этап",
                checks = listOf(
                    CheckItem("1.1", "Открыть А-401"),
                    CheckItem("1.2", "Давление", CheckType.PARAM, "500 кгс/см²"),
                ),
                positions = listOf("А-401", "PV-10603"),
            ),
            Phase(
                id = 2,
                section = "Раздел А",
                title = "Второй этап",
                condition = "Только при длительной остановке",
                critical = true,
                checks = listOf(CheckItem("2.1", "Закрыть 14047")),
                positions = listOf("14047"),
                timer = TimerSpec(40, "Циркуляция"),
            ),
            Phase(
                id = 3,
                section = "Раздел Б",
                title = "Третий этап",
                checks = listOf(CheckItem("3.1", "Продуть Б-402"), CheckItem("3.2", "Сбросить давление")),
                positions = listOf("Б-402"),
            ),
        ),
        trainerQuestions = listOf(
            TrainerQuestion("q1", 1, "Вопрос 1", listOf("а", "б"), 0),
            TrainerQuestion("q2", 1, "Вопрос 2", listOf("а", "б"), 1),
            TrainerQuestion("q3", 3, "Вопрос 3", listOf("а", "б"), 0),
            TrainerQuestion("q4", null, "Вопрос 4", listOf("а", "б"), 1),
        ),
    )
}
