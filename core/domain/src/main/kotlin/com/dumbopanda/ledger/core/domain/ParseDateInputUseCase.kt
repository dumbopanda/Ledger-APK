package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.common.LedgerClock
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

/** §7.5.1's parsing table, rule by rule, in the order the table gives them. */
sealed interface DateFieldState {
    data object Empty : DateFieldState

    data class Error(val message: String) : DateFieldState

    data class Resolved(
        val date: LocalDate,
        // True only when no year was typed at all (the "search back up to 8 years" branch) --
        // an explicit 2- or 4-digit year is never "inferred", even if it lands in the past.
        val yearInferred: Boolean,
        val daysAgo: Long,
    ) : DateFieldState
}

class ParseDateInputUseCase @Inject constructor(
    private val clock: LedgerClock,
) {
    operator fun invoke(rawInput: String): DateFieldState {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return DateFieldState.Empty

        val parts = tokenize(trimmed)
        if (parts.any { it.isEmpty() || !it.all(Char::isDigit) }) {
            return DateFieldState.Error("Numbers only")
        }
        if (parts.size == 1) {
            return DateFieldState.Error("Add the month too, like 5.9")
        }
        if (parts.size > 3) {
            return DateFieldState.Error("Too many parts")
        }

        val dayPart = parts[0]
        val monthPart = parts[1]
        val yearPart = parts.getOrNull(2)
        if (dayPart.length > 2 || monthPart.length > 2) {
            return DateFieldState.Error("Day and month are 1 or 2 digits")
        }

        val month = monthPart.toInt()
        if (month !in 1..12) return DateFieldState.Error("There is no month $month")

        val day = dayPart.toInt()
        if (day !in 1..31) return DateFieldState.Error("There is no day $day")

        // Structural feasibility check, independent of any specific year: catches "31 April"
        // (day/month combos that are never valid) but lets 29 February through, since whether
        // THAT is real depends on which year it eventually resolves to.
        if (!isPossibleDayMonth(day, month)) {
            return DateFieldState.Error("There is no $day ${monthName(month)}")
        }

        // Local calendar day, not UTC -- "today" for a spend is whatever day it is on the phone.
        // LocalDate.ofInstant is API 34+; atZone(...).toLocalDate() does the same thing back to
        // API 26 (this project's minSdk).
        val today = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()

        val resolved: LocalDate
        val yearInferred: Boolean
        when {
            yearPart == null -> {
                yearInferred = true
                resolved = mostRecentPastOccurrence(day, month, today)
                    ?: return DateFieldState.Error("There is no $day ${monthName(month)}")
            }
            yearPart.length == 2 -> {
                yearInferred = false
                val year = mostRecentYearEndingIn(yearPart.toInt(), today.year)
                resolved = dateOrNull(year, month, day)
                    ?: return DateFieldState.Error("There is no $day ${monthName(month)} $year")
            }
            yearPart.length == 4 -> {
                yearInferred = false
                val year = yearPart.toInt()
                resolved = dateOrNull(year, month, day)
                    ?: return DateFieldState.Error("There is no $day ${monthName(month)} $year")
            }
            else -> return DateFieldState.Error("Year should be 2 or 4 digits")
        }

        if (resolved.isAfter(today)) {
            return DateFieldState.Error("That date is still in the future")
        }

        return DateFieldState.Resolved(
            date = resolved,
            yearInferred = yearInferred,
            daysAgo = ChronoUnit.DAYS.between(resolved, today),
        )
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (char in input) {
            if (char == '.' || char == '-' || char == '/' || char.isWhitespace()) {
                tokens += current.toString()
                current.clear()
            } else {
                current.append(char)
            }
        }
        tokens += current.toString()
        return tokens
    }

    private fun isPossibleDayMonth(day: Int, month: Int): Boolean = dateOrNull(LEAP_REFERENCE_YEAR, month, day) != null

    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }

    private fun mostRecentPastOccurrence(day: Int, month: Int, today: LocalDate): LocalDate? {
        for (yearsBack in 0..YEAR_SEARCH_WINDOW) {
            val candidate = dateOrNull(today.year - yearsBack, month, day) ?: continue
            if (!candidate.isAfter(today)) return candidate
        }
        return null
    }

    private fun mostRecentYearEndingIn(twoDigitYear: Int, currentYear: Int): Int {
        val century = (currentYear / 100) * 100
        var candidate = century + twoDigitYear
        while (candidate > currentYear) candidate -= 100
        return candidate
    }

    private fun monthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    private companion object {
        // 2000 is a leap year and lets 29 February pass the generic day/month feasibility
        // check; whether it's real for the ACTUAL year is decided later, once that's known.
        const val LEAP_REFERENCE_YEAR = 2000
        const val YEAR_SEARCH_WINDOW = 8
    }
}
