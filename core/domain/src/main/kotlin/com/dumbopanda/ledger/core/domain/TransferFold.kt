package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection

// §7.7's "transfer folding": two legs render as one line, matched by `transfer_group`.
sealed interface HistoryRow {
    data class Single(val entry: Entry) : HistoryRow
    data class Transfer(val sent: Entry, val received: Entry) : HistoryRow
}

/**
 * Folds a list of entries (already ordered newest-first, typically one [DayGroup]'s worth)
 * into [HistoryRow]s, pairing a transfer's two legs by `transferGroup`.
 *
 * Deliberately page-local: [entries] is whatever window the caller hands in (one Paging3 page,
 * or one day group's entries), and a sibling leg outside that window is invisible to this
 * function by construction -- it falls back to rendering that entry as [HistoryRow.Single]
 * rather than crashing or blocking. In practice a transfer's two legs share both `entryDate`
 * and the same save transaction's `createdAt` window, so they land adjacent in the newest-
 * first order `EntryDao.pagingSource()` produces and are overwhelmingly likely to share a
 * page; a leg split across a page boundary is a known, rare, documented simplification, not
 * a silently-accepted bug.
 */
fun foldTransfers(entries: List<Entry>): List<HistoryRow> {
    val consumed = mutableSetOf<String>()
    val rows = mutableListOf<HistoryRow>()
    for (entry in entries) {
        if (entry.id in consumed) continue
        val group = entry.transferGroup
        val sibling = if (group != null) entries.firstOrNull { it.transferGroup == group && it.id != entry.id } else null
        if (group == null || sibling == null) {
            rows += HistoryRow.Single(entry)
            continue
        }
        consumed += entry.id
        consumed += sibling.id
        val sent = if (entry.direction == TxnDirection.Debit) entry else sibling
        val received = if (entry.direction == TxnDirection.Debit) sibling else entry
        rows += HistoryRow.Transfer(sent, received)
    }
    return rows
}
