package com.dumbopanda.ledger.core.model

// A Y-ID that currently appears an odd number of times across live entries —
// "money expected back" that hasn't come back yet (§2).
@JvmInline
value class OpenYId(val txnId: TxnId) {
    init { require(txnId.isY) }
}
