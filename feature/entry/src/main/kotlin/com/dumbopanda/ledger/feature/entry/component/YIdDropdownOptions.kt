package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.model.TxnId

/**
 * §7.5.5's Y-dropdown contents: a freshly generated ID first, then every currently open
 * Y-ID -- except in edit mode, where the entry's OWN stored ID must survive even if it isn't
 * (or is no longer) open, "inserted at the top rather than lost".
 */
fun buildYIdDropdownOptions(currentTxnId: TxnId, openYIds: List<TxnId>, freshId: TxnId): List<TxnId> {
    val options = mutableListOf<TxnId>()
    if (currentTxnId.isY && currentTxnId != freshId && currentTxnId !in openYIds) options.add(currentTxnId)
    options.add(freshId)
    options.addAll(openYIds)
    return options
}
