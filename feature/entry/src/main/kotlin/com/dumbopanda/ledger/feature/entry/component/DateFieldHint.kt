package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.domain.DateFieldState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// §7.5.1's hint-line states, as a pure mapping from the use case's result to display text --
// kept separate from the DateField composable so the exact wording is testable without Compose.
sealed interface DateFieldHint {
    // Field is empty: the placeholder copy, not a warning.
    data class Placeholder(val text: String) : DateFieldHint

    // A resolved date that needs no caveat -- rendered blank, but the line still holds its
    // height so nothing jumps when a warning line would otherwise appear.
    data object None : DateFieldHint

    data class Warning(val text: String) : DateFieldHint

    data class Error(val text: String) : DateFieldHint
}

// index.html's formatFullDate: en-GB, so September is "Sept" -- "Sat 27 Sept 2025".
private val WARNING_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
private const val DAYS_AGO_WARNING_THRESHOLD = 90

fun computeDateFieldHint(dateState: DateFieldState, today: LocalDate): DateFieldHint = when (dateState) {
    DateFieldState.Empty -> DateFieldHint.Placeholder("Day and month, like 5.9. Ledger works out the year.")
    is DateFieldState.Error -> DateFieldHint.Error(dateState.message)
    is DateFieldState.Resolved -> {
        val formatted = dateState.date.format(WARNING_DATE_FORMAT)
        // A no-year guess that landed outside the current year: literally "last year" in the
        // plan's own wording, even on the rare multi-year jump (an 8-year-apart 29 Feb search) --
        // the spec gives no separate phrasing for "N years ago", so this folds into the same case.
        when {
            dateState.yearInferred && dateState.date.year != today.year ->
                DateFieldHint.Warning("$formatted, last year")
            dateState.daysAgo > DAYS_AGO_WARNING_THRESHOLD ->
                DateFieldHint.Warning("$formatted, ${dateState.daysAgo} days ago")
            else -> DateFieldHint.None
        }
    }
}
