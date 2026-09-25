package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate

/**
 * `ui-spec.md`'s "Will I get this back?" Old picker: "lists currently-open items by expense
 * name (with amount and date)" -- unlike the old raw-ID dropdown, a choice here carries enough
 * to render that, not just the bare [TxnId].
 */
sealed interface TxnIdChoice {
    val txnId: TxnId

    // The "New" option -- a freshly minted id, nothing to show but that it's new.
    data class Fresh(override val txnId: TxnId) : TxnIdChoice

    // An "Old" option -- either a currently-open item, or (edit mode only) the entry's own
    // stored id when it is no longer open and so has no [OwedToMeItem] of its own.
    data class Existing(
        override val txnId: TxnId,
        val expenseName: String,
        val amount: Money,
        val date: LocalDate,
    ) : TxnIdChoice
}

/**
 * Wraps [buildYIdDropdownOptions]'s ordering (fresh id first, then every open id, with an
 * edit-mode current-but-closed id inserted at the top) with the display data each choice
 * needs. [editFallback], when non-null, supplies the label for exactly one case: editing an
 * entry whose stored Y-id is no longer open -- it has no [OwedToMeItem], so there is nothing
 * to look it up in, and the entry's own already-known fields are the only source left.
 */
fun buildTxnIdChoices(
    currentTxnId: TxnId,
    openItems: List<OwedToMeItem>,
    freshId: TxnId,
    editFallback: TxnIdChoice.Existing? = null,
): List<TxnIdChoice> {
    val byId = openItems.associateBy { it.txnId }
    val ordered = buildYIdDropdownOptions(currentTxnId, openItems.map { it.txnId }, freshId)
    return ordered.map { id ->
        when {
            id == freshId -> TxnIdChoice.Fresh(id)
            byId[id] != null -> byId.getValue(id).let { TxnIdChoice.Existing(id, it.expenseName, it.amount, it.openedDate) }
            editFallback != null && editFallback.txnId == id -> editFallback
            else -> TxnIdChoice.Fresh(id) // defensive only -- every other case is covered above
        }
    }
}
