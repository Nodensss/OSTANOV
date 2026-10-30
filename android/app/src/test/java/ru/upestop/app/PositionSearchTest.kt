package ru.upestop.app

import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.domain.PositionSearch

class PositionSearchTest {
    @Test fun normalizedPositionsAndOperationText() {
        val phases = realAlgorithm().phases
        listOf("a-401", "А401", "10603", "14047").forEach { query -> assertTrue(query, PositionSearch.filter(phases, query).isNotEmpty()) }
        val synthetic = phases.first().copy(positions = listOf("PV-10603"), checks = emptyList())
        assertEquals(listOf(synthetic), PositionSearch.filter(listOf(synthetic), "pv-10603"))
        assertEquals(phases, PositionSearch.filter(phases, ""))
        assertTrue(PositionSearch.filter(phases, "zzzzzzzz").isEmpty())
        assertEquals(PositionSearch.normalize("А–401"), PositionSearch.normalize(" a — 4 0 1 "))
        assertEquals("авсенкмортху", PositionSearch.normalize("ABCEHKMOPTXY"))
        val phrase = phases.first().checks.first().text
        assertTrue(PositionSearch.filter(phases, phrase).contains(phases.first()))
        val paramPhase = phases.first { p -> p.checks.any { it.param != null } }
        assertTrue(PositionSearch.filter(phases, paramPhase.checks.first { it.param != null }.param!!).contains(paramPhase))
    }
}
