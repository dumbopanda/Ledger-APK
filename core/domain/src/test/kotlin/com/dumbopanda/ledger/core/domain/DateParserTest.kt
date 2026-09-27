package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.common.LedgerClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// §7.5.1's parsing table, one (or a few) test per row. "Today" is pinned to a fixed,
// deliberately unremarkable Sunday so every case is deterministic regardless of when
// the suite actually runs.
class DateParserTest {
    private val today = LocalDate.of(2025, 6, 15)

    private fun useCaseAsOf(date: LocalDate): ParseDateInputUseCase {
        val instant = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        return ParseDateInputUseCase(LedgerClock { instant })
    }

    private val useCase = useCaseAsOf(today)

    private fun assertError(input: String, message: String) {
        val result = useCase(input)
        assertEquals(DateFieldState.Error(message), result)
    }

    private fun assertResolved(
        input: String,
        expectedDate: LocalDate,
        yearInferred: Boolean,
        useCase: ParseDateInputUseCase = this.useCase,
        asOf: LocalDate = today,
    ) {
        val result = useCase(input)
        val expectedDaysAgo = ChronoUnit.DAYS.between(expectedDate, asOf)
        assertEquals(
            DateFieldState.Resolved(expectedDate, yearInferred, expectedDaysAgo),
            result,
        )
    }

    // -- Empty --------------------------------------------------------------

    @Test
    fun `empty string is the Empty hint state`() {
        assertEquals(DateFieldState.Empty, useCase(""))
    }

    @Test
    fun `whitespace only is also Empty`() {
        assertEquals(DateFieldState.Empty, useCase("   "))
    }

    // -- Non-digit (empty) part ----------------------------------------------

    @Test
    fun `doubled separator leaves an empty part`() {
        assertError("5..9", "Numbers only")
    }

    @Test
    fun `leading separator leaves an empty leading part`() {
        assertError(".5.9", "Numbers only")
    }

    @Test
    fun `trailing separator leaves an empty trailing part`() {
        assertError("5.9.", "Numbers only")
    }

    @Test
    fun `a letter inside a part is rejected defensively`() {
        assertError("5a.9", "Numbers only")
    }

    // -- One part only --------------------------------------------------------

    @Test
    fun `a single number with no separator asks for the month`() {
        assertError("5", "Add the month too, like 5.9")
    }

    @Test
    fun `three digits with no separator is still one part, not day-plus-month`() {
        assertError("212", "Add the month too, like 5.9")
    }

    // -- Too many parts ---------------------------------------------------------

    @Test
    fun `four parts is too many`() {
        assertError("5.9.2025.1", "Too many parts")
    }

    // -- Day or month over 2 digits ----------------------------------------------

    @Test
    fun `a 3-digit day part is rejected before range-checking it`() {
        assertError("555.9", "Day and month are 1 or 2 digits")
    }

    @Test
    fun `a 3-digit month part is rejected before range-checking it`() {
        assertError("5.999", "Day and month are 1 or 2 digits")
    }

    // -- Month range -------------------------------------------------------------

    @Test
    fun `month 13 does not exist`() {
        assertError("5.13", "There is no month 13")
    }

    @Test
    fun `month 0 does not exist`() {
        assertError("5.0", "There is no month 0")
    }

    // -- Day range -----------------------------------------------------------------

    @Test
    fun `day 32 does not exist`() {
        assertError("32.5", "There is no day 32")
    }

    @Test
    fun `day 0 does not exist`() {
        assertError("0.5", "There is no day 0")
    }

    // -- Structurally impossible day-month combos, regardless of year --------------

    @Test
    fun `31 April is never real`() {
        assertError("31.4", "There is no 31 April")
    }

    @Test
    fun `30 February is never real`() {
        assertError("30.2", "There is no 30 February")
    }

    @Test
    fun `31 June is never real`() {
        assertError("31.6", "There is no 31 June")
    }

    // -- Explicit 2-digit year --------------------------------------------------

    @Test
    fun `explicit 2-digit year in the past resolves against that year`() {
        assertResolved("5.5.25", LocalDate.of(2025, 5, 5), yearInferred = false)
    }

    @Test
    fun `explicit 2-digit year that lands later this year is still future`() {
        assertError("5.9.25", "That date is still in the future")
    }

    @Test
    fun `explicit 2-digit year that would be next year rolls back a century, never forward`() {
        // today is 2025 -- "26" can't mean 2026 (future), so it means 1926.
        assertResolved("5.9.26", LocalDate.of(1926, 9, 5), yearInferred = false)
    }

    @Test
    fun `29 Feb with an explicit 2-digit leap year resolves`() {
        assertResolved("29.2.24", LocalDate.of(2024, 2, 29), yearInferred = false)
    }

    @Test
    fun `29 Feb with an explicit 2-digit non-leap year is not a real date`() {
        assertError("29.2.25", "There is no 29 February 2025")
    }

    @Test
    fun `explicit 2-digit year landing exactly on today resolves with zero days ago`() {
        assertResolved("15.6.25", today, yearInferred = false)
    }

    @Test
    fun `explicit 2-digit year landing tomorrow is future`() {
        assertError("16.6.25", "That date is still in the future")
    }

    // -- Explicit 4-digit year --------------------------------------------------

    @Test
    fun `explicit 4-digit year in the future is rejected`() {
        assertError("5.9.2026", "That date is still in the future")
    }

    @Test
    fun `explicit 4-digit year in the past resolves`() {
        assertResolved("5.9.2020", LocalDate.of(2020, 9, 5), yearInferred = false)
    }

    @Test
    fun `29 Feb with an explicit 4-digit non-leap year is not a real date`() {
        assertError("29.2.2023", "There is no 29 February 2023")
    }

    @Test
    fun `29 Feb with an explicit 4-digit leap year resolves`() {
        assertResolved("29.2.2024", LocalDate.of(2024, 2, 29), yearInferred = false)
    }

    @Test
    fun `a very old explicit 4-digit year is taken as given, not clamped`() {
        assertResolved("5.9.0001", LocalDate.of(1, 9, 5), yearInferred = false)
    }

    // -- Year length other than 2 or 4 -------------------------------------------

    @Test
    fun `a 1-digit year is rejected`() {
        assertError("5.9.1", "Year should be 2 or 4 digits")
    }

    @Test
    fun `a 3-digit year is rejected`() {
        assertError("5.9.202", "Year should be 2 or 4 digits")
    }

    @Test
    fun `a 5-digit year is rejected`() {
        assertError("5.9.20256", "Year should be 2 or 4 digits")
    }

    // -- No year -- most recent past occurrence ----------------------------------

    @Test
    fun `no year, date already happened today, resolves to today`() {
        assertResolved("15.6", today, yearInferred = true)
    }

    @Test
    fun `no year, date has not happened yet this year, falls back to last year`() {
        assertResolved("16.6", LocalDate.of(2024, 6, 16), yearInferred = true)
    }

    @Test
    fun `no year, date already passed earlier this year, stays this year`() {
        assertResolved("1.1", LocalDate.of(2025, 1, 1), yearInferred = true)
    }

    @Test
    fun `no year, 29 Feb in a non-leap current year searches back to the last leap year`() {
        assertResolved("29.2", LocalDate.of(2024, 2, 29), yearInferred = true)
    }

    @Test
    fun `no year, 29 Feb needs the full 8-year window when leap years are 8 apart`() {
        // 2100 is not a leap year (divisible by 100, not 400), so the leap years bracketing
        // it are 2096 and 2104 -- 8 years apart. From 2103, the nearest PAST 29 Feb is 2096,
        // seven years back: the case the 8-year search window exists for.
        val asOf = LocalDate.of(2103, 1, 1)
        assertResolved(
            "29.2",
            LocalDate.of(2096, 2, 29),
            yearInferred = true,
            useCase = useCaseAsOf(asOf),
            asOf = asOf,
        )
    }

    @Test
    fun `no year, slash separator behaves the same as dot`() {
        assertResolved("16/6", LocalDate.of(2024, 6, 16), yearInferred = true)
    }

    // -- Separator equivalence ----------------------------------------------------

    @Test
    fun `dash separators work the same as dots`() {
        assertResolved("5-9-2020", LocalDate.of(2020, 9, 5), yearInferred = false)
    }

    @Test
    fun `slash separators work the same as dots`() {
        assertResolved("5/9/2020", LocalDate.of(2020, 9, 5), yearInferred = false)
    }

    @Test
    fun `space separators work the same as dots`() {
        assertResolved("5 9 2020", LocalDate.of(2020, 9, 5), yearInferred = false)
    }

    // -- daysAgo arithmetic ---------------------------------------------------------

    @Test
    fun `days ago is computed correctly for a date over 90 days back`() {
        val result = useCase("1.3.2025")
        assertTrue(result is DateFieldState.Resolved)
        assertEquals(106L, (result as DateFieldState.Resolved).daysAgo)
        assertTrue(result.daysAgo > 90)
    }

    @Test
    fun `days ago is computed correctly for a date under 90 days back`() {
        val result = useCase("1.6.2025")
        assertTrue(result is DateFieldState.Resolved)
        assertEquals(14L, (result as DateFieldState.Resolved).daysAgo)
    }

    // -- yearInferred distinguishes explicit-year-equals-today from inferred --------

    @Test
    fun `an explicit year landing on today is not flagged as inferred, unlike the no-year case`() {
        val result = useCase("15.6.2025")
        assertEquals(DateFieldState.Resolved(today, yearInferred = false, daysAgo = 0), result)
    }

    // -- Robustness -------------------------------------------------------------------

    @Test
    fun `surrounding whitespace on the whole input is trimmed`() {
        assertResolved("  5.9.2020  ", LocalDate.of(2020, 9, 5), yearInferred = false)
    }

    @Test
    fun `a 2-digit year of 99 that is not future stays in the 1900s`() {
        assertResolved("1.1.99", LocalDate.of(1999, 1, 1), yearInferred = false)
    }

    @Test
    fun `new year's eve is an ordinary valid past date`() {
        assertResolved("31.12.2024", LocalDate.of(2024, 12, 31), yearInferred = false)
    }
}
