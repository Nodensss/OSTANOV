package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private val algorithm = TestData.sample()
    private val flat = algorithm.flatChecks()

    @Test
    fun flatListKeepsPhaseOrder() {
        assertEquals(listOf("1.1", "1.2", "2.1", "3.1", "3.2"), flat.map { it.check.id })
        assertEquals(listOf(0, 1, 2, 3, 4), flat.map { it.index })
        assertEquals(listOf("Раздел А", "Раздел Б"), algorithm.sections())
    }

    @Test
    fun countsIgnoreUnknownIds() {
        val checked = mapOf("1.1" to 1L, "9.9" to 2L)
        assertEquals(1, Progress.completed(algorithm, checked))
        assertEquals(5, algorithm.totalChecks)
        assertEquals(20, Progress.percent(1, 5))
        assertEquals(46, Progress.percent(45, 97))
        assertEquals(0, Progress.percent(0, 0))
    }

    @Test
    fun phaseDone() {
        val phase = algorithm.phases[0]
        assertFalse(Progress.isPhaseDone(phase, mapOf("1.1" to 1L)))
        assertTrue(Progress.isPhaseDone(phase, mapOf("1.1" to 1L, "1.2" to 2L)))
        assertEquals(1, Progress.phaseCompleted(phase, mapOf("1.1" to 1L)))
    }

    @Test
    fun currentPosition() {
        assertEquals("1.1", Progress.firstUnchecked(flat, emptyMap())?.check?.id)
        val checked = mapOf("1.1" to 1L, "1.2" to 2L)
        assertEquals(2, Progress.currentPhase(algorithm, checked)?.id)
        val all = flat.associate { it.check.id to 1L }
        assertNull(Progress.currentPhase(algorithm, all))
    }

    @Test
    fun nextUncheckedWrapsToStart() {
        // Отмечены 1.2, 2.1 и 3.x: после 2.1 неотмеченных нет — возвращаемся к первой неотмеченной 1.1.
        val checked = mapOf("1.2" to 1L, "2.1" to 2L, "3.1" to 3L, "3.2" to 4L)
        assertEquals("1.1", Progress.nextUnchecked(flat, checked, afterIndex = 2)?.check?.id)
        assertEquals("2.1", Progress.nextUnchecked(flat, mapOf("1.1" to 1L, "1.2" to 2L), afterIndex = 0)?.check?.id)
        assertNull(Progress.nextUnchecked(flat, flat.associate { it.check.id to 1L }, afterIndex = 0))
    }

    @Test
    fun markRange() {
        assertNull(Progress.markRange(algorithm, emptyMap()))
        assertEquals(10L to 30L, Progress.markRange(algorithm, mapOf("1.1" to 30L, "3.2" to 10L, "x" to 99L)))
    }
}
