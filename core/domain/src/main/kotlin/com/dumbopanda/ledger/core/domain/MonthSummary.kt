package com.dumbopanda.ledger.core.domain

// §7.7's month summary card: Out / In / Net for the current calendar month, transfers
// excluded ("moving your own money is not spending" -- the same rule GroupEntriesByDayUseCase
// applies per day). Both minor-unit sums are computed server-side by the SQL query
// (EntryDao.outMinorBetween/inMinorBetween), never re-summed here -- netMinor is the only
// derived value, and it's cheap enough to just be a property.
data class MonthSummary(val outMinor: Long, val inMinor: Long) {
    val netMinor: Long get() = inMinor - outMinor
}
