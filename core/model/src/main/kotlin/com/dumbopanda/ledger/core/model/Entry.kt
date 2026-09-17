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
        require(expenseName.trim().length in 1..120)
        require(txnType.trim().length in 1..60)
        require(moreInfo == null || moreInfo.length <= 300)
    }
}
