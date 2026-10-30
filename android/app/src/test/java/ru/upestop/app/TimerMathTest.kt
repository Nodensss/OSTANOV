package ru.upestop.app

import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.domain.*

class TimerMathTest {
    @Test fun lifecycleAndClamping() {
        val duration = TimerMath.duration(40)
        val initial = TimerState(9)
        val started = TimerMath.start(initial, 1000)
        assertEquals(1000L, started.firstStartedAt)
        assertEquals(30 * 60_000L, TimerMath.remaining(started, duration, 601000))
        val paused = TimerMath.pause(started, duration, 601000)
        assertFalse(paused.running)
        assertEquals(30 * 60_000L, TimerMath.remaining(paused, duration, 9999999))
        val resumed = TimerMath.start(paused, 700000)
        assertEquals(1000L, resumed.firstStartedAt)
        assertEquals(29 * 60_000L, TimerMath.remaining(resumed, duration, 760000))
        assertEquals(duration, TimerMath.elapsed(resumed, duration, Long.MAX_VALUE / 2))
        assertEquals(paused.elapsedMs, TimerMath.elapsed(resumed, duration, 1))
        val finished = TimerMath.finish(resumed, duration, 2500000)
        assertTrue(finished.finished); assertFalse(finished.running)
        assertEquals(2500000L, finished.finishedAt)
        assertEquals(0L, TimerMath.remaining(finished, duration, 3000000))
        assertThrows(IllegalArgumentException::class.java) { TimerMath.start(finished, 3000000) }
        assertEquals(initial, TimerMath.reset(finished))
    }
    @Test fun displayAndTestDuration() {
        assertEquals("40:00", TimerMath.format(2400000))
        assertEquals("3:00:00", TimerMath.format(10800000))
        assertEquals("00:01", TimerMath.format(1))
        assertEquals("00:00", TimerMath.format(0))
        assertEquals(40000L, TimerMath.duration(40, true))
    }
}
