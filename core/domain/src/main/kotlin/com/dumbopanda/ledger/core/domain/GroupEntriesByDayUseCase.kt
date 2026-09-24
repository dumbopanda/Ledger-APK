package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection
import java.time.LocalDate
import javax.inject.Inject

data class DayGroup(
    val date: LocalDate,
    val entries: List<Entry>,
    // Transfers excluded -- "moving your own money is not spending" (§7.7), the same rule
    // the month summary card and core:database's `netMinorBetween` query both already apply.
    val netMinor: Long,
)

/**
 * §7.7: "entries grouped by day ... each day header carries that day's net." Assumes
 * [entries] already arrives sorted newest-day-first, newest-entry-first-within-a-day (the
 * order `EntryDao.pagingSource()` produces) -- `groupBy` preserves first-seen key order, so no
 * re-sorting happens here, and a caller that hands in unsorted input gets unsorted groups back.
 */
class GroupEntriesByDayUseCase @Inject constructor() {
    fun group(entries: List<Entry>): List<DayGroup> =
        entries.groupBy(Entry::entryDate).map { (date, dayEntries) ->
            val netMinor = dayEntries
                .filter { it.transferGroup == null }
                .sumOf { entry ->
                    when (entry.direction) {
                        TxnDirection.Credit -> entry.amount.minor
                        TxnDirection.Debit -> -entry.amount.minor
                    }
                }
            DayGroup(date, dayEntries, netMinor)
        }
}
