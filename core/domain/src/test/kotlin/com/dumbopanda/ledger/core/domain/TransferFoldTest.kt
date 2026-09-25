package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TransferFoldTest {
    private fun entry(
        id: String,
        direction: TxnDirection = TxnDirection.Debit,
        transferGroup: String? = null,
        amountMinor: Long = 10_000,
    ) = Entry(
        id = id,
        clientUid = "client-$id",
        entryDate = LocalDate.of(2026, 9, 24),
        txnId = TxnId("Nabc123"),
        accountId = "account-1",
        expenseName = "Entry $id",
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.Monthly,
        amount = Money(amountMinor),
        direction = direction,
        transferGroup = transferGroup,
        createdAt = Instant.parse("2026-09-24T00:00:00Z"),
    )

    @Test
    fun `a non-transfer entry folds to a Single row`() {
        val rows = foldTransfers(listOf(entry("1")))
        assertEquals(listOf(HistoryRow.Single(entry("1"))), rows)
    }

    @Test
    fun `two legs sharing a transfer group fold into one Transfer row`() {
        val sent = entry("1", TxnDirection.Debit, transferGroup = "group-1")
        val received = entry("2", TxnDirection.Credit, transferGroup = "group-1")
        val rows = foldTransfers(listOf(sent, received))
        assertEquals(listOf(HistoryRow.Transfer(sent, received)), rows)
    }

    @Test
    fun `folding is direction-independent -- credit leg listed first still resolves sent-received correctly`() {
        val sent = entry("1", TxnDirection.Debit, transferGroup = "group-1")
        val received = entry("2", TxnDirection.Credit, transferGroup = "group-1")
        val rows = foldTransfers(listOf(received, sent))
        assertEquals(listOf(HistoryRow.Transfer(sent, received)), rows)
    }

    @Test
    fun `a transfer leg with no sibling in the window falls back to a Single row`() {
        val orphan = entry("1", transferGroup = "group-1")
        val rows = foldTransfers(listOf(orphan))
        assertEquals(listOf(HistoryRow.Single(orphan)), rows)
    }

    @Test
    fun `a mix of transfers and plain entries folds correctly and preserves order`() {
        val plain1 = entry("1")
        val sent = entry("2", TxnDirection.Debit, transferGroup = "group-1")
        val received = entry("3", TxnDirection.Credit, transferGroup = "group-1")
        val plain2 = entry("4")
        val rows = foldTransfers(listOf(plain1, sent, received, plain2))
        assertEquals(
            listOf(HistoryRow.Single(plain1), HistoryRow.Transfer(sent, received), HistoryRow.Single(plain2)),
            rows,
        )
    }

    @Test
    fun `each leg of a pair is consumed exactly once, even if it appears first`() {
        val sent = entry("1", TxnDirection.Debit, transferGroup = "group-1")
        val received = entry("2", TxnDirection.Credit, transferGroup = "group-1")
        val rows = foldTransfers(listOf(sent, received))
        assertTrue(rows.size == 1)
    }
}
