package com.dumbopanda.ledger.core.domain

import javax.inject.Inject

/** The clean (digits + at most one `.`, at most 2 decimals) text of an amount field, plus a
 * caret position measured in THAT string's coordinates -- never the grouped display string's. */
data class AmountFieldValue(val clean: String, val caret: Int)

/**
 * §7.5.8 -- "the highest-risk field in the app." Two separate jobs, both pure:
 * 1. [applyKeystroke] enforces the field's limits on the clean (ungrouped) text, refusing an
 *    edit outright (not truncating it after the fact) if it would exceed them.
 * 2. [toDisplay] / [cleanToDisplayOffset] / [displayToCleanOffset] map that clean text to an
 *    `en-IN`-grouped display string and back, in both directions -- what a Compose
 *    `OffsetMapping` needs so the caret survives the transformation.
 */
class FormatAmountUseCase @Inject constructor() {

    fun applyKeystroke(old: AmountFieldValue, rawText: String, rawCaret: Int): AmountFieldValue {
        val filtered = filterAmountChars(rawText, rawCaret)
        val dotIndex = filtered.clean.indexOf('.')
        val integerPart = if (dotIndex == -1) filtered.clean else filtered.clean.substring(0, dotIndex)
        val decimalPart = if (dotIndex == -1) null else filtered.clean.substring(dotIndex + 1)

        // Refused at the keystroke, never accepted then truncated/rounded.
        if (integerPart.length > MAX_INTEGER_DIGITS) return old
        if (decimalPart != null && decimalPart.length > MAX_DECIMAL_DIGITS) return old

        // "0" + "5" -> "5", but a lone "0" survives (it's what makes "0.5" possible).
        val leadingZerosStripped = integerPart.length - integerPart.trimStart('0').length
        val strippedCount = leadingZerosStripped.coerceAtMost(integerPart.length - 1).coerceAtLeast(0)
        val newIntegerPart = integerPart.substring(strippedCount)
        val newClean = newIntegerPart + if (decimalPart != null) ".$decimalPart" else ""

        val caretShift = strippedCount.coerceAtMost(filtered.caret)
        val newCaret = (filtered.caret - caretShift).coerceIn(0, newClean.length)

        return AmountFieldValue(newClean, newCaret)
    }

    fun toDisplay(clean: String): String {
        val dotIndex = clean.indexOf('.')
        val integerPart = if (dotIndex == -1) clean else clean.substring(0, dotIndex)
        val tail = if (dotIndex == -1) "" else clean.substring(dotIndex) // includes the '.'
        return groupIndian(integerPart) + tail
    }

    fun cleanToDisplayOffset(clean: String, cleanOffset: Int): Int {
        val display = toDisplay(clean)
        var seen = 0
        for (i in display.indices) {
            if (seen == cleanOffset) return i
            if (display[i] != ',') seen++
        }
        return display.length
    }

    fun displayToCleanOffset(clean: String, displayOffset: Int): Int {
        val display = toDisplay(clean)
        val bounded = displayOffset.coerceIn(0, display.length)
        return display.substring(0, bounded).count { it != ',' }
    }

    // ">= 1,00,000 | readback note under the field: '5 lakh'." No spec precision beyond the
    // single whole-number example, so this truncates (never rounds up past the real amount)
    // to 2 decimal places and drops a trailing ".00"/"0".
    fun lakhReadback(clean: String): String? {
        val rupees = clean.substringBefore('.').toLongOrNull() ?: return null
        if (rupees < LAKH) return null
        val hundredths = (rupees * 100L) / LAKH // rupees/LAKH scaled by 100, integer-exact
        val whole = hundredths / 100
        val frac = hundredths % 100
        val text = when {
            frac == 0L -> whole.toString()
            frac % 10 == 0L -> "$whole.${frac / 10}"
            else -> "$whole.${frac.toString().padStart(2, '0')}"
        }
        return "$text lakh"
    }

    private fun groupIndian(integerPart: String): String {
        val n = integerPart.length
        if (n <= 3) return integerPart
        val sb = StringBuilder()
        for (i in 0 until n) {
            val digitsToTheRightInclusive = n - i
            if (i > 0 && digitsToTheRightInclusive >= 3 && (digitsToTheRightInclusive - 3) % 2 == 0) {
                sb.append(',')
            }
            sb.append(integerPart[i])
        }
        return sb.toString()
    }

    private fun filterAmountChars(rawText: String, rawCaret: Int): AmountFieldValue {
        val boundedCaret = rawCaret.coerceIn(0, rawText.length)
        var dotSeen = false
        val sb = StringBuilder()
        var caretAfterFilter = 0
        for ((index, char) in rawText.withIndex()) {
            val keep = when {
                char.isDigit() -> true
                char == '.' && !dotSeen -> { dotSeen = true; true }
                else -> false
            }
            if (keep) {
                sb.append(char)
                if (index < boundedCaret) caretAfterFilter++
            }
        }
        return AmountFieldValue(sb.toString(), caretAfterFilter)
    }

    private companion object {
        const val MAX_INTEGER_DIGITS = 7
        const val MAX_DECIMAL_DIGITS = 2
        const val LAKH = 100_000L
    }
}
