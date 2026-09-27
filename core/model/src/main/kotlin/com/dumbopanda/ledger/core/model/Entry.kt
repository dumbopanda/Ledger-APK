package com.dumbopanda.ledger.core.model

import java.time.Instant
import java.time.LocalDate

data class Entry(
    val id: String,
    val clientUid: String,
    val entryDate: LocalDate,
    val txnId: TxnId,
    val accountId: String,
    val expenseName: String,
    val moreInfo: String?,
    val txnType: String,
    val txnFreq: TxnFrequency,
    val amount: Money,
    val direction: TxnDirection,
    val transferGroup: String?,
    val createdAt: Instant,
) {
    init {
        require(clientUid.length in 8..64)
        require(entryDate >= MIN_ENTRY_DATE)
        require(expenseName.trim().length in 1..120)
        require(txnType.trim().length in 1..60)
        require(moreInfo == null || moreInfo.length <= 300)
    }

    companion object {
        private val MIN_ENTRY_DATE: LocalDate = LocalDate.of(2000, 1, 1)
    }
}

/**
 * An account's starting balance is stored as one ordinary entry with this name and category, so
 * it syncs, backs up and counts toward balances like any other -- but it is neither spending nor
 * income, so Stats leaves it out.
 */
const val OPENING_BALANCE = "Opening balance"

val Entry.isOpeningBalance: Boolean get() = txnType == OPENING_BALANCE

// A leg of a loan ("Will I get this back?"): money lent out or coming back. It moves a balance,
// but it isn't spending or income. A self transfer shares the Y id shape, so it's excluded.
val Entry.isLoanLeg: Boolean get() = txnId.isY && transferGroup == null
