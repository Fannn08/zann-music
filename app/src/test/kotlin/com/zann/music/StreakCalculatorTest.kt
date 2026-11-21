package com.zann.music

import com.zann.music.data.stats.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 11, 21)

    @Test fun empty() {
        val r = StreakCalculator.compute(emptySet(), today)
        assertEquals(0, r.current); assertEquals(0, r.longest)
    }

    @Test fun runEndingToday() {
        val r = StreakCalculator.compute(setOf(today, today.minusDays(1), today.minusDays(2)), today)
        assertEquals(3, r.current); assertTrue(r.todayActive)
    }

    @Test fun streakSurvivesUntilEndOfToday() {
        val r = StreakCalculator.compute(setOf(today.minusDays(1), today.minusDays(2)), today)
        assertEquals(2, r.current); assertFalse(r.todayActive)
    }

    @Test fun brokenStreakResets() {
        val r = StreakCalculator.compute(setOf(today.minusDays(3), today.minusDays(4)), today)
        assertEquals(0, r.current); assertEquals(2, r.longest)
    }

    @Test fun longestIsKept() {
        val old = (10L..16L).map { today.minusDays(it) }.toSet()
        val r = StreakCalculator.compute(old + today, today)
        assertEquals(1, r.current); assertEquals(7, r.longest)
    }
}
