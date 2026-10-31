package ru.upestop.app.domain

import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm

/** Этап с совпадениями: какие позиции и операции подошли под запрос. */
data class PhaseMatch(val phase: Phase, val positions: Set<String>, val checkIds: Set<String>)

object PositionSearch {

    /** Латинские буквы, неотличимые на вид от кириллических. Нормализуются обе стороны сравнения. */
    private val latinToCyrillic = mapOf(
        'a' to 'а', 'b' to 'в', 'c' to 'с', 'e' to 'е', 'h' to 'н', 'k' to 'к',
        'm' to 'м', 'o' to 'о', 'p' to 'р', 't' to 'т', 'x' to 'х', 'y' to 'у',
        'ё' to 'е',
    )
    private val ignored = setOf('-', '–', '—')

    fun normalize(value: String): String = buildString {
        for (char in value.lowercase()) {
            if (char.isWhitespace() || char in ignored) continue
            append(latinToCyrillic[char] ?: char)
        }
    }

    /** Поиск по позициям, тексту и параметру операций. Пустой запрос — все этапы без подсветки. */
    fun search(algorithm: StopAlgorithm, query: String): List<PhaseMatch> {
        val needle = normalize(query)
        if (needle.isEmpty()) return algorithm.phases.map { PhaseMatch(it, emptySet(), emptySet()) }

        return algorithm.phases.mapNotNull { phase ->
            val positions = phase.positions.filter { normalize(it).contains(needle) }.toSet()
            val checkIds = phase.checks
                .filter { check ->
                    normalize(check.text).contains(needle) ||
                        check.param?.let { normalize(it).contains(needle) } == true
                }
                .map { it.id }
                .toSet()
            if (positions.isEmpty() && checkIds.isEmpty()) null else PhaseMatch(phase, positions, checkIds)
        }
    }
}
