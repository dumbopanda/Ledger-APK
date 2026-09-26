package com.dumbopanda.ledger.core.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// index.html formats every date with `toLocaleDateString('en-GB', ...)` and every amount with
// `toLocaleString('en-IN', ...)`. These are those exact formats, in one place, so every screen
// says "26 Sept 2026" and "₹4,500" the same way the web does.

private val UK = Locale.UK
private val DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", UK) // 26 Sept 2026
private val WEEKDAY_LONG = DateTimeFormatter.ofPattern("EEEE", UK) // Saturday
private val DAY_SAME_YEAR = DateTimeFormatter.ofPattern("EEE d MMM", UK) // Fri 25 Sept
private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", UK) // September 2026
private val MONTH_NAME = DateTimeFormatter.ofPattern("MMMM", UK) // September

private val grouper = FormatAmountUseCase()

/** `money(n)` -- whole rupees print bare ("4,500"), anything else to two places ("4,500.50"). */
fun formatMoney(minor: Long): String {
    val abs = kotlin.math.abs(minor)
    val rupees = abs / 100
    val paise = abs % 100
    val body = if (paise == 0L) grouper.toDisplay(rupees.toString()) else grouper.toDisplay("$rupees.${paise.toString().padStart(2, '0')}")
    return if (minor < 0) "-$body" else body
}

/** `toLocaleString('en-IN', { minimumFractionDigits: 2 })` -- always two places ("4,500.00"). */
fun formatMoneyFixed(minor: Long): String {
    val abs = kotlin.math.abs(minor)
    val body = grouper.toDisplay("${abs / 100}.${(abs % 100).toString().padStart(2, '0')}")
    return if (minor < 0) "-$body" else body
}

/** A clean amount-field string ("4500", "4500.5") in minor units, or null if it isn't one. */
fun cleanToMinor(clean: String): Long? {
    if (clean.isBlank()) return null
    val dot = clean.indexOf('.')
    val whole = (if (dot == -1) clean else clean.substring(0, dot)).ifEmpty { "0" }.toLongOrNull() ?: return null
    val frac = (if (dot == -1) "" else clean.substring(dot + 1)).padEnd(2, '0').take(2).toLongOrNull() ?: return null
    return whole * 100 + frac
}

/** `formatDisplayDate` -- "26 Sept 2026". */
fun formatDisplayDate(date: LocalDate): String = date.format(DISPLAY_DATE)

/** `formatConfirmDate` -- "26 Sept 2026 (Saturday)". */
fun formatConfirmDate(date: LocalDate): String = "${date.format(DISPLAY_DATE)} (${date.format(WEEKDAY_LONG)})"

/** `dayLabel` -- History's day headers: Today, Yesterday, "Fri 25 Sept", or "25 Sept 2025". */
fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(DAY_SAME_YEAR)
    else -> date.format(DISPLAY_DATE)
}

/** `monthLabelFor` -- "September 2026". */
fun monthLabel(month: YearMonth): String = month.format(MONTH_LABEL)

/** Just the month's name -- "Show earlier in September". */
fun monthName(month: YearMonth): String = month.format(MONTH_NAME)

/** `monthSubFor` -- the line under the month toggle's label. */
fun monthSub(offset: Int): String = when (offset) {
    0 -> "Current month"
    -1 -> "Last month"
    else -> "${-offset} months ago"
}
