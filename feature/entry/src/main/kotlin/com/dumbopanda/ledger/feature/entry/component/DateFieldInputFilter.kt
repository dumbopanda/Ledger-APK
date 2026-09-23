package com.dumbopanda.ledger.feature.entry.component

// §7.5.1: "digits and `. - / space` only. Anything else is dropped and the caret is put
// back where it was, not thrown to the end." Pure and Compose-free so it's testable
// without a TextFieldValue: `rawCaret` is where the IME left the cursor in `rawText`
// (i.e. after whatever character was just typed, allowed or not).
data class FilteredDateInput(val text: String, val caret: Int)

private val ALLOWED_CHARS = setOf('.', '-', '/', ' ')

fun filterDateFieldInput(rawText: String, rawCaret: Int): FilteredDateInput {
    val boundedCaret = rawCaret.coerceIn(0, rawText.length)
    // How many allowed characters sit before the caret is exactly where the caret
    // belongs once the disallowed ones (if any) are removed -- that's what "put back
    // where it was" means for a filter that only ever deletes characters.
    val caretAfterFilter = rawText.substring(0, boundedCaret).count(::isAllowedDateChar)
    val filteredText = rawText.filter(::isAllowedDateChar)
    return FilteredDateInput(filteredText, caretAfterFilter)
}

private fun isAllowedDateChar(char: Char): Boolean = char.isDigit() || char in ALLOWED_CHARS
