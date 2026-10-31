package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionSearchTest {
    private val sample = TestData.sample()
    private val repository = TestData.repositoryAlgorithm

    @Test
    fun latinLookalikesMatchCyrillic() {
        assertEquals(PositionSearch.normalize("А-401"), PositionSearch.normalize("a-401"))
        val match = PositionSearch.search(sample, "a-401").single()
        assertEquals(1, match.phase.id)
        assertEquals(setOf("А-401"), match.positions)
    }

    @Test
    fun separatorsAndCaseAreIgnored() {
        assertEquals(setOf("А-401"), PositionSearch.search(sample, "А401").single().positions)
        assertEquals(setOf("PV-10603"), PositionSearch.search(sample, "pv-10603").single().positions)
        assertEquals(setOf("PV-10603"), PositionSearch.search(sample, " Pv 10603 ").single().positions)
    }

    @Test
    fun numbersAndCheckText() {
        val byNumber = PositionSearch.search(sample, "14047").single()
        assertEquals(2, byNumber.phase.id)
        assertEquals(setOf("14047"), byNumber.positions)
        assertEquals(setOf("2.1"), byNumber.checkIds)

        val byText = PositionSearch.search(sample, "продуть").single()
        assertEquals(setOf("3.1"), byText.checkIds)
        assertTrue(byText.positions.isEmpty())

        assertEquals(setOf("1.2"), PositionSearch.search(sample, "500 кгс").single().checkIds)
    }

    @Test
    fun emptyAndMissingQueries() {
        assertEquals(sample.phases.map { it.id }, PositionSearch.search(sample, "  ").map { it.phase.id })
        assertTrue(PositionSearch.search(sample, "нет-такой-позиции").isEmpty())
    }

    @Test
    fun repositoryDataLatinQueryFindsCyrillicPosition() {
        val expected = repository.phases.filter { phase -> phase.positions.contains("А-401") }.map { it.id }
        assertTrue(expected.isNotEmpty())
        val found = PositionSearch.search(repository, "a-401").filter { "А-401" in it.positions }.map { it.phase.id }
        assertEquals(expected, found)
    }
}
