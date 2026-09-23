package com.dumbopanda.ledger.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// §7.5.8 / §10.1: 7-digit ceiling, 3rd decimal refused, leading-zero rules, en-IN grouping,
// caret preserved through the grouped display string, lakh readback.
class AmountFormatterTest {
    private val useCase = FormatAmountUseCase()

    // -- Keystroke limits: refused outright, not truncated after the fact ------------------

    @Test
    fun `up to 7 integer digits is accepted`() {
        val result = useCase.applyKeystroke(AmountFieldValue("", 0), "1234567", 7)
        assertEquals(AmountFieldValue("1234567", 7), result)
    }

    @Test
    fun `an 8th integer digit is refused at the keystroke, the whole edit reverts`() {
        val old = AmountFieldValue("1234567", 7)
        val result = useCase.applyKeystroke(old, "12345678", 8)
        assertEquals(old, result)
    }

    @Test
    fun `a 3rd decimal digit is refused, never accepted then rounded`() {
        val old = AmountFieldValue("5.12", 4)
        val result = useCase.applyKeystroke(old, "5.123", 5)
        assertEquals(old, result)
    }

    @Test
    fun `exactly 2 decimal digits is accepted`() {
        val result = useCase.applyKeystroke(AmountFieldValue("5.1", 3), "5.12", 4)
        assertEquals(AmountFieldValue("5.12", 4), result)
    }

    // -- Leading zeros ------------------------------------------------------------------------

    @Test
    fun `0 then 5 becomes 5, not 05`() {
        val result = useCase.applyKeystroke(AmountFieldValue("0", 1), "05", 2)
        assertEquals(AmountFieldValue("5", 1), result)
    }

    @Test
    fun `a lone 0 survives on its own`() {
        val result = useCase.applyKeystroke(AmountFieldValue("", 0), "0", 1)
        assertEquals(AmountFieldValue("0", 1), result)
    }

    @Test
    fun `a lone 0 before a dot survives, making 0 point 5 possible`() {
        val result = useCase.applyKeystroke(AmountFieldValue("0", 1), "0.5", 3)
        assertEquals(AmountFieldValue("0.5", 3), result)
    }

    @Test
    fun `multiple leading zeros collapse to the first significant digit, caret adjusts`() {
        val result = useCase.applyKeystroke(AmountFieldValue("00", 2), "007", 3)
        assertEquals(AmountFieldValue("7", 1), result)
    }

    // -- Character filtering: letters, extra dots, caret preserved on reject -----------------

    @Test
    fun `a letter typed mid-string is dropped and the caret does not jump to the end`() {
        // "125" with the caret after "1" (index 1), typing "a" -> IME hands us "1a25", caret 2.
        val result = useCase.applyKeystroke(AmountFieldValue("125", 1), "1a25", 2)
        assertEquals(AmountFieldValue("125", 1), result)
    }

    @Test
    fun `a second decimal point is dropped, the first one stands`() {
        val old = AmountFieldValue("5.12", 4)
        val result = useCase.applyKeystroke(old, "5.12.", 5)
        assertEquals(old, result)
    }

    @Test
    fun `pasting garbage that would smuggle in a 3rd decimal digit is refused whole`() {
        val old = AmountFieldValue("", 0)
        // Filtering "1.2.3abc4" keeps only the first dot: "1.234" -- 3 decimal digits, refused.
        val result = useCase.applyKeystroke(old, "1.2.3abc4", 9)
        assertEquals(old, result)
    }

    // -- en-IN grouping -------------------------------------------------------------------------

    @Test
    fun `empty string displays as empty`() {
        assertEquals("", useCase.toDisplay(""))
    }

    @Test
    fun `3 or fewer digits are never grouped`() {
        assertEquals("123", useCase.toDisplay("123"))
    }

    @Test
    fun `4 digits get one comma`() {
        assertEquals("1,234", useCase.toDisplay("1234"))
    }

    @Test
    fun `5 digits group as 2 then 3`() {
        assertEquals("12,345", useCase.toDisplay("12345"))
    }

    @Test
    fun `6 digits group as 1,2,3`() {
        assertEquals("1,23,456", useCase.toDisplay("123456"))
    }

    @Test
    fun `7 digits match the spec's own example`() {
        assertEquals("12,34,567", useCase.toDisplay("1234567"))
    }

    @Test
    fun `8 digits (beyond the field's own ceiling) still group correctly as a pure function`() {
        assertEquals("1,00,00,000", useCase.toDisplay("10000000"))
    }

    @Test
    fun `decimals are preserved but never grouped`() {
        assertEquals("12,34,567.89", useCase.toDisplay("1234567.89"))
    }

    @Test
    fun `a lone-zero integer part with decimals is not grouped`() {
        assertEquals("0.5", useCase.toDisplay("0.5"))
    }

    // -- Offset mapping: clean coordinates to display coordinates, and back ------------------

    @Test
    fun `clean offset 0 maps to display offset 0`() {
        assertEquals(0, useCase.cleanToDisplayOffset("1234567", 0))
    }

    @Test
    fun `clean offset at the end maps to the display string's end`() {
        assertEquals(9, useCase.cleanToDisplayOffset("1234567", 7)) // "12,34,567" is 9 chars
    }

    @Test
    fun `a clean offset right before a comma boundary maps to just before the comma`() {
        // "1234567" -> "12,34,567". Clean offset 2 (after "12") maps to display index 2.
        assertEquals(2, useCase.cleanToDisplayOffset("1234567", 2))
    }

    @Test
    fun `a display offset landing on a comma still resolves to the clean offset before it`() {
        assertEquals(2, useCase.displayToCleanOffset("1234567", 2))
        assertEquals(2, useCase.displayToCleanOffset("1234567", 3)) // just past the comma too
    }

    @Test
    fun `insert, delete, and mid-string edit all round-trip through the display offset`() {
        val clean = "1234567"
        for (cleanOffset in 0..clean.length) {
            val displayOffset = useCase.cleanToDisplayOffset(clean, cleanOffset)
            assertEquals(cleanOffset, useCase.displayToCleanOffset(clean, displayOffset))
        }
    }

    @Test
    fun `offsets inside the decimal part shift by a constant, no grouping applies there`() {
        val clean = "1234567.89"
        val display = useCase.toDisplay(clean) // "12,34,567.89"
        val cleanOffsetAtLastDigit = clean.length
        assertEquals(display.length, useCase.cleanToDisplayOffset(clean, cleanOffsetAtLastDigit))
    }

    // -- Lakh readback --------------------------------------------------------------------------

    @Test
    fun `below 1 lakh has no readback`() {
        assertNull(useCase.lakhReadback("50000"))
    }

    @Test
    fun `exactly 1 lakh reads back as a whole number`() {
        assertEquals("1 lakh", useCase.lakhReadback("100000"))
    }

    @Test
    fun `5 lakh matches the spec's own example exactly`() {
        assertEquals("5 lakh", useCase.lakhReadback("500000"))
    }

    @Test
    fun `a non-round amount reads back with up to 2 decimal places`() {
        assertEquals("12.34 lakh", useCase.lakhReadback("1234567"))
    }

    @Test
    fun `the field's own maximum reads back correctly`() {
        assertEquals("99.99 lakh", useCase.lakhReadback("9999999"))
    }

    @Test
    fun `a readback with an exact one-decimal value drops the trailing zero`() {
        // 1,50,000 -> 1.5 lakh, not "1.50 lakh".
        assertEquals("1.5 lakh", useCase.lakhReadback("150000"))
    }
}
