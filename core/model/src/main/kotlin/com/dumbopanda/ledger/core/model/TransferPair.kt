package com.dumbopanda.ledger.core.model

// The two legs of a transfer between the user's own accounts (§2): one debit,
// one credit, equal amounts, same date, sharing a transfer_group. Deleting
// one deletes both — this type exists so callers can't hold just one leg.
data class TransferPair(val out: Entry, val into: Entry) {
    init {
        require(out.transferGroup != null && out.transferGroup == into.transferGroup)
        require(out.direction == TxnDirection.Debit && into.direction == TxnDirection.Credit)
        require(out.amount == into.amount)
        require(out.entryDate == into.entryDate)
    }

    val group get() = requireNotNull(out.transferGroup)
}
