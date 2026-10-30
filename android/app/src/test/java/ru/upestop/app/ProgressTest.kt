package ru.upestop.app

import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.domain.*

class ProgressTest {
    @Test fun unknownIdsCompletionAndWrapAround() {
        val data = realAlgorithm()
        val first = data.phases.first()
        val checks = first.checks.map { CheckState(it.id, true, 1) } + CheckState("removed", true, 2)
        val progress = Progress(data, checks)
        assertEquals(first.checks.size, progress.count)
        assertTrue(progress.phaseDone(first))
        assertFalse(progress.phaseDone(data.phases[1]))
        assertEquals(data.phases[1], progress.currentPhase)
        assertEquals(first.checks.size, progress.firstIndex)
        assertEquals(first.checks.size, progress.nextUnchecked(progress.total - 1))
        assertEquals(first.checks.size + 1, progress.nextUnchecked(first.checks.size))
        val done = Progress(data, data.phases.flatMap { it.checks }.map { CheckState(it.id, true, 1) })
        assertEquals(-1, done.firstIndex); assertNull(done.currentPhase); assertEquals(-1, done.nextUnchecked(0))
    }
}
