package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate

/**
 * `ui-spec.md`'s "Owed to me" row, changed 24-25 Sep 2026: one list, every Y-id ever seen --
 * open or closed -- shown once, split into an Open card and a Closed card. `isOpen` is what
 * decides which card a row belongs to; a closed row additionally carries [closedDate] and a
 * second entry id, since the toggle in the entry-detail sheet (added 25 Sep 2026) needs to
 * reach either leg independently.
 *
 * Display fields (name/amount/account) come from the MORE RECENT of the two legs when closed
 * -- the close (repayment) leg -- matching the entry-detail sheet's own default ("opens the
 * close leg by default, since that's the more likely one to need a fix").
 */
data class OwedToMeItem(
    val txnId: TxnId,
    val isOpen: Boolean,
    val expenseName: String,
    val amount: Money,
    val accountId: String,
    val openedDate: LocalDate,
    val closedDate: LocalDate?,
    // The open leg's own entry id -- always present, an open item has exactly one leg.
    val openLegEntryId: String,
    // Present only once closed; the entry id of the leg that closed the pair.
    val closeLegEntryId: String?,
)
