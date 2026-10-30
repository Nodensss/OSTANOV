package ru.upestop.app

import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.AlgorithmValidator

class AlgorithmJsonTest {
    @Test fun realAssetIsCompleteAndValid() {
        val text = File(System.getProperty("algorithm.json")).readText()
        val raw = AlgorithmJson.parseToJsonElement(text).jsonObject
        val data = AlgorithmJson.decodeFromString<StopAlgorithm>(text)
        assertEquals(raw.getValue("phases").jsonArray.size, data.phases.size)
        assertEquals(raw.getValue("phases").jsonArray.sumOf { it.jsonObject.getValue("checks").jsonArray.size }, data.phases.sumOf { it.checks.size })
        assertEquals(raw.getValue("criticalRules").jsonArray.size, data.criticalRules.size)
        assertEquals(raw.getValue("trainerQuestions").jsonArray.size, data.trainerQuestions.size)
        assertEquals(emptyList<String>(), AlgorithmValidator.validate(data))
        raw.getValue("phases").jsonArray.forEachIndexed { i, phase ->
            assertEquals(phase.jsonObject["critical"]?.jsonPrimitive?.boolean ?: false, data.phases[i].critical)
        }
    }
}
