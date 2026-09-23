package com.dumbopanda.ledger.feature.entry.component

import org.junit.Assert.assertEquals
import org.junit.Test

// §7.5.1's "digits and `. - / space` only, caret put back where it was" rule, plus the
// three by-hand cases the plan calls out (§10.5-adjacent, B10's check): select-all-delete,
// mid-string edit, partial input preserved. Automated here so they don't rely solely on a
// real device.
class DateFieldInputFilterTest {

    @Test
    fun `plain digits pass through untouched`() {
        assertEquals(FilteredDateInput("259", 3), filterDateFieldInput("259", 3))
    }

    @Test
    fun `allowed separators pass through untouched`() {
        assertEquals(FilteredDateInput("25.9.2026", 9), filterDateFieldInput("25.9.2026", 9))
    }

    @Test
    fun `a rejected letter typed at the end is dropped and the caret stays at the text's end`() {
        // "25.9" then typing "a" -> IME hands us "25.9a" with caret at 5.
        assertEquals(FilteredDateInput("25.9", 4), filterDateFieldInput("25.9a", 5))
    }

    @Test
    fun `a rejected letter typed mid-string is dropped and the caret does not jump to the end`() {
        // "12/25" with caret after "12", typing "a" -> IME hands us "12a/25" with caret at 3.
        assertEquals(FilteredDateInput("12/25", 2), filterDateFieldInput("12a/25", 3))
    }

    @Test
    fun `select-all-delete leaves empty text and a zero caret`() {
        assertEquals(FilteredDateInput("", 0), filterDateFieldInput("", 0))
    }

    @Test
    fun `mid-string edit -- deleting one digit out of the middle keeps the rest and the caret`() {
        // "25/09/2026", caret after the first "0" (index 4), backspace -> "25/9/2026", caret 3.
        assertEquals(FilteredDateInput("25/9/2026", 3), filterDateFieldInput("25/9/2026", 3))
    }

    @Test
    fun `partial input is preserved, not cleared, while still unparseable`() {
        assertEquals(FilteredDateInput("2", 1), filterDateFieldInput("2", 1))
    }

    @Test
    fun `pasting mixed garbage strips everything disallowed and recomputes the caret`() {
        // "25/Sep/2026" keeps both slashes (only the letters "Sep" are stripped) -> "25//2026".
        assertEquals(FilteredDateInput("25//2026", 8), filterDateFieldInput("25/Sep/2026", 11))
    }

    @Test
    fun `a caret in the middle of a paste is placed after only the allowed prefix`() {
        // Same paste, but the raw caret sits right after "25/Se" (index 5) -- only "25/"
        // in that prefix is allowed, so the filtered caret is 3.
        assertEquals(FilteredDateInput("25//2026", 3), filterDateFieldInput("25/Sep/2026", 5))
    }

    @Test
    fun `consecutive separators are not characters this filter rejects -- that is the parser's job`() {
        assertEquals(FilteredDateInput("5..9", 4), filterDateFieldInput("5..9", 4))
    }

    @Test
    fun `an out-of-range caret is clamped instead of throwing`() {
        assertEquals(FilteredDateInput("25", 2), filterDateFieldInput("25", 99))
    }
}
