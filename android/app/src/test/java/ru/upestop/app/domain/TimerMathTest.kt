package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMathTest {
    private val minute = TimerMath.MINUTE_MS
    private val duration = TimerMath.durationMs(40, testMode = false)
    private val t0 = 1_700_000_000_000L
    private val idle = TimerState(phaseId = 9)

    @Test
    fun durations() {
        assertEquals(40 * minute, duration)
        assertEquals(40_000L, TimerMath.durationMs(40, testMode = true))
    }

    @Test
    fun startCountsDown() {
        val running = TimerMath.start(idle, duration, t0)
        assertTrue(running.running)
        assertEquals(t0, running.firstStartedAt)
        assertEquals(duration, running.durationMs)
        assertEquals(30 * minute, TimerMath.remaining(running, duration, t0 + 10 * minute))
        assertEquals(t0 + duration, TimerMath.endsAt(running, duration))
    }

    @Test
    fun pauseFreezesAndResumeContinues() {
        val running = TimerMath.start(idle, duration, t0)
        val paused = TimerMath.pause(running, t0 + 10 * minute)
        assertTrue(paused.paused)
        assertNull(TimerMath.endsAt(paused, duration))
        assertEquals(30 * minute, TimerMath.remaining(paused, duration, t0 + 25 * minute))

        val resumed = TimerMath.start(paused, duration, t0 + 30 * minute)
        assertEquals(t0, resumed.firstStartedAt)
        assertEquals(20 * minute, TimerMath.remaining(resumed, duration, t0 + 40 * minute))
        assertEquals(t0 + 60 * minute, TimerMath.endsAt(resumed, duration))
    }

    @Test
    fun elapsedNeverExceedsDuration() {
        val running = TimerMath.start(idle, duration, t0)
        assertEquals(duration, TimerMath.elapsed(running, duration, t0 + 3 * duration))
        assertEquals(0L, TimerMath.remaining(running, duration, t0 + 3 * duration))
        assertTrue(TimerMath.isExpired(running, duration, t0 + duration))
        assertFalse(TimerMath.isExpired(running, duration, t0 + duration - 1))
    }

    @Test
    fun finishAndRestartRules() {
        val running = TimerMath.start(idle, duration, t0)
        val finished = TimerMath.finish(running, duration, t0 + duration)
        assertTrue(finished.finished)
        assertFalse(finished.running)
        assertEquals(t0 + duration, finished.finishedAt)
        assertEquals(0L, TimerMath.remaining(finished, duration, t0 + 2 * duration))
        // Завершённый таймер не перезапускается без сброса.
        assertSame(finished, TimerMath.start(finished, duration, t0 + 2 * duration))
        assertSame(finished, TimerMath.pause(finished, t0 + 2 * duration))
    }

    @Test
    fun resetReturnsToIdle() {
        val running = TimerMath.start(idle, duration, t0)
        assertEquals(idle, TimerMath.reset(running))
        assertTrue(TimerMath.reset(running).idle)
    }

    @Test
    fun durationFixedAtStart() {
        val testDuration = TimerMath.durationMs(40, testMode = true)
        val running = TimerMath.start(idle, testDuration, t0)
        assertEquals(testDuration, TimerMath.effectiveDuration(running, 40, testMode = false))
        assertEquals(duration, TimerMath.effectiveDuration(idle, 40, testMode = false))
    }

    @Test
    fun formatting() {
        assertEquals("40:00", TimerMath.formatRemaining(40 * minute))
        assertEquals("3:00:00", TimerMath.formatRemaining(180 * minute))
        assertEquals("00:01", TimerMath.formatRemaining(1))
        assertEquals("00:00", TimerMath.formatRemaining(0))
        assertEquals("59:59", TimerMath.formatRemaining(59 * minute + 58_001))
        assertEquals("1:12:30", TimerMath.formatElapsed(72 * minute + 30_999))
        assertEquals("0:45:00", TimerMath.formatElapsed(45 * minute))
    }
}
