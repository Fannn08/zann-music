package com.zann.music.data.stats

import java.time.LocalDate

data class StreakInfo(
    val current: Int,
    val longest: Int,
    val todayActive: Boolean,
    val activeDays: Int,
)

/** Pure streak logic (no Android), so it is easy to unit test. */
object StreakCalculator {
    /**
     * A day is "active" when the user listened long enough. The current streak is the run of
     * consecutive active days ending today, or ending yesterday while today is still open
     * (so the flame stays alive until the day is over).
     */
    fun compute(active: Set<LocalDate>, today: LocalDate): StreakInfo {
        if (active.isEmpty()) return StreakInfo(0, 0, false, 0)
        val todayActive = today in active
        var cursor = if (todayActive) today else today.minusDays(1)
        var current = 0
        while (cursor in active) {
            current++
            cursor = cursor.minusDays(1)
        }
        val sorted = active.sorted()
        var longest = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i - 1].plusDays(1) == sorted[i]) {
                run++
                if (run > longest) longest = run
            } else run = 1
        }
        return StreakInfo(current, maxOf(longest, current), todayActive, active.size)
    }
}
