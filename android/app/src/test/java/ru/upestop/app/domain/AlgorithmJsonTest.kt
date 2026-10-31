package ru.upestop.app.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.upestop.app.data.model.CheckType

/** Проверки настоящего data/stop-algorithm.json из корня репозитория. */
class AlgorithmJsonTest {
    private val algorithm = TestData.repositoryAlgorithm
    private val raw = Json.parseToJsonElement(TestData.repositoryJson).jsonObject

    @Test
    fun modelKeepsEverythingFromRawJson() {
        val rawPhases = raw.getValue("phases").jsonArray
        assertEquals(rawPhases.size, algorithm.phases.size)
        assertEquals(
            rawPhases.sumOf { it.jsonObject.getValue("checks").jsonArray.size },
            algorithm.totalChecks,
        )
        assertEquals(
            rawPhases.sumOf { it.jsonObject.getValue("positions").jsonArray.size },
            algorithm.phases.sumOf { it.positions.size },
        )
        assertEquals(raw.getValue("criticalRules").jsonArray.size, algorithm.criticalRules.size)
        assertEquals(raw.getValue("trainerQuestions").jsonArray.size, algorithm.trainerQuestions.size)
        assertEquals(raw.getValue("sources").jsonArray.size, algorithm.sources.size)
    }

    @Test
    fun optionalPhaseFieldsAreParsed() {
        val rawPhases = raw.getValue("phases").jsonArray.map { it.jsonObject }
        fun idsWith(key: String) = rawPhases.filter { key in it }.map { it.getValue("id").jsonPrimitive.content.toInt() }

        assertEquals(idsWith("timer"), algorithm.phases.filter { it.timer != null }.map { it.id })
        assertEquals(idsWith("scheme"), algorithm.phases.filter { it.scheme != null }.map { it.id })
        assertEquals(idsWith("condition"), algorithm.phases.filter { it.condition != null }.map { it.id })
        assertEquals(idsWith("critical"), algorithm.phases.filter { it.critical }.map { it.id })
        assertEquals(
            rawPhases.sumOf { (it["warnings"] as? JsonArray)?.size ?: 0 },
            algorithm.phases.sumOf { it.warnings.size },
        )
    }

    @Test
    fun checkTypesMatchRawJson() {
        val rawParams = raw.getValue("phases").jsonArray
            .flatMap { it.jsonObject.getValue("checks").jsonArray }
            .count { it.jsonObject["type"]?.jsonPrimitive?.content == "param" }
        assertEquals(rawParams, algorithm.phases.flatMap { it.checks }.count { it.type == CheckType.PARAM })
    }

    @Test
    fun repositoryJsonPassesValidation() {
        assertEquals(emptyList<ValidationError>(), AlgorithmValidator.validate(algorithm))
    }

    @Test
    fun algorithmHasContentForEveryMode() {
        assertTrue(algorithm.phases.isNotEmpty())
        assertTrue(algorithm.criticalRules.isNotEmpty())
        assertTrue(algorithm.trainerQuestions.isNotEmpty())
        assertTrue(algorithm.phases.any { it.timer != null })
    }
}
