package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import javax.inject.Inject

/**
 * `ui-spec.md` History > Owed to me: folds every live Y-prefixed [Entry] into one
 * [OwedToMeItem] per `txn_id`, open or closed. §7.9's odd/even rule still decides open vs
 * closed (`isOpen` = an odd leg count) -- this just additionally carries the display and
 * open/close-toggle fields the new UI needs, which the old bare-id computation didn't.
 *
 * In the ordinary case a `txn_id` has exactly one leg (open) or two (closed, an open leg
 * paired with its repayment). A caller can only ever pair a NEW entry against a currently-open
 * id (`ValidateEntryDraftUseCase`'s upstream picker only offers those), so more than two legs
 * for one id is not a shape the app itself produces -- if it's ever seen anyway (e.g. imported
 * data), the earliest leg is still `openedDate`/`openLegEntryId` and the most recent is still
 * the representative/`closedDate`/`closeLegEntryId`; anything in between is folded in without
 * its own slot, a documented simplification rather than a crash.
 */
class GroupOwedToMeUseCase @Inject constructor() {
    fun group(entries: List<Entry>): List<OwedToMeItem> =
        entries
            // A self transfer's legs share a Y id too, but nobody owes anything -- they're not loans.
            .filter { it.txnId.isY && it.transferGroup == null }
            .groupBy { it.txnId }
            .map { (txnId, legs) ->
                val ordered = legs.sortedWith(compareBy(Entry::entryDate, Entry::createdAt))
                val earliest = ordered.first()
                val mostRecent = ordered.last()
                val isOpen = ordered.size % 2 == 1
                OwedToMeItem(
                    txnId = txnId,
                    isOpen = isOpen,
                    expenseName = mostRecent.expenseName,
                    amount = mostRecent.amount,
                    accountId = mostRecent.accountId,
                    openedDate = earliest.entryDate,
                    closedDate = if (isOpen) null else mostRecent.entryDate,
                    openLegEntryId = earliest.id,
                    closeLegEntryId = if (isOpen) null else mostRecent.id,
                )
            }
}
