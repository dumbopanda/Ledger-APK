package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.domain.DateFieldState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFieldHintTest {
    private val today = LocalDate.of(2026, 9, 24)

    @Test
    fun `empty state shows the placeholder copy`() {
        assertEquals(
            DateFieldHint.Placeholder("Day and month, separated — the year is worked out"),
            computeDateFieldHint(DateFieldState.Empty, today),
        )
    }

    @Test
    fun `an error state's message passes through unchanged`() {
        assertEquals(
            DateFieldHint.Error("There is no month 14"),
            computeDateFieldHint(DateFieldState.Error("There is no month 14"), today),
        )
    }

    @Test
    fun `a normal recent resolved date holds its height with no warning`() {
        val state = DateFieldState.Resolved(LocalDate.of(2026, 9, 20), yearInferred = true, daysAgo = 4)
        assertEquals(DateFieldHint.None, computeDateFieldHint(state, today))
    }

    @Test
    fun `an explicit year exactly 90 days back is still not a warning`() {
        val date = today.minusDays(90)
        val state = DateFieldState.Resolved(date, yearInferred = false, daysAgo = 90)
        assertEquals(DateFieldHint.None, computeDateFieldHint(state, today))
    }

    @Test
    fun `year inferred to last year warns with the exact spec wording`() {
        val date = LocalDate.of(2025, 12, 25)
        val state = DateFieldState.Resolved(date, yearInferred = true, daysAgo = 273)
        assertEquals(
            DateFieldHint.Warning("Thu 25 Dec 2025 — last year"),
            computeDateFieldHint(state, today),
        )
    }

    @Test
    fun `more than 90 days ago warns with the exact spec wording`() {
        // 3 Mar 2026 is a real Tuesday, not the spec example's illustrative "Mon" -- the
        // weekday must come from the formatter, not be hand-typed, so this pins the actual day.
        val date = LocalDate.of(2026, 3, 3)
        val state = DateFieldState.Resolved(date, yearInferred = false, daysAgo = 104)
        assertEquals(
            DateFieldHint.Warning("Tue 3 Mar 2026 — 104 days ago"),
            computeDateFieldHint(state, today),
        )
    }

    @Test
    fun `an explicit year in the past does not get the last-year wording, only the days-ago one`() {
        // yearInferred is false here -- an explicit year is never flagged as "inferred",
        // even when it's last year, so only the days-ago threshold can trigger a warning.
        val date = LocalDate.of(2025, 12, 25)
        val state = DateFieldState.Resolved(date, yearInferred = false, daysAgo = 273)
        assertEquals(
            DateFieldHint.Warning("Thu 25 Dec 2025 — 273 days ago"),
            computeDateFieldHint(state, today),
        )
    }
}
