package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.model.Account
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

class EditDraftTest {
    private val domestic = Account("acc-1", "client-acc-1", "Domestic", 0, null)
    private val cash = Account("acc-2", "client-acc-2", "Cash", 1, null)
    private val accounts = listOf(domestic, cash)

    private fun entry(
        id: String,
        accountId: String = domestic.id,
        direction: TxnDirection = TxnDirection.Debit,
        transferGroup: String? = null,
        txnType: String = "Payment",
        expenseName: String = "Groceries",
        createdAt: Instant = Instant.parse("2026-09-01T00:00:00Z"),
        txnId: TxnId = TxnId("Nabc123"),
    ) = Entry(
        id = id,
        clientUid = "client-$id",
        entryDate = LocalDate.of(2026, 9, 14),
        txnId = txnId,
        accountId = accountId,
        expenseName = expenseName,
        moreInfo = "note",
        txnType = txnType,
        txnFreq = TxnFrequency.Monthly,
        amount = Money(450_000),
        direction = direction,
        transferGroup = transferGroup,
        createdAt = createdAt,
    )

    @Test
    fun `a plain entry builds a non-transfer draft and a Plain original`() {
        val e = entry("1")
        val result = buildEditDraft(e, sibling = null, accounts = accounts)

        assertTrue(!result.draft.selfTransfer)
        assertEquals(e.expenseName, result.draft.expenseName)
        assertEquals(e.accountId, result.draft.accountId)
        assertEquals(e.txnType, result.draft.txnType)
        assertEquals(false, result.draft.editModeOriginalIsTransfer)
        assertEquals(EditingOriginal.Plain("1", "client-1", e.createdAt), result.original)
    }

    @Test
    fun `a plain entry's toAccountId falls back to a different existing account`() {
        val e = entry("1", accountId = domestic.id)
        val result = buildEditDraft(e, sibling = null, accounts = accounts)
        assertEquals(cash.id, result.draft.toAccountId)
    }

    @Test
    fun `a transfer builds a self-transfer draft with both leg types and a Transfer original`() {
        val debit = entry("1", accountId = domestic.id, direction = TxnDirection.Debit, transferGroup = "group-1", txnType = "Sent Transfer")
        val credit = entry("2", accountId = cash.id, direction = TxnDirection.Credit, transferGroup = "group-1", txnType = "Received Transfer")

        val result = buildEditDraft(debit, sibling = credit, accounts = accounts)

        assertTrue(result.draft.selfTransfer)
        assertEquals(domestic.id, result.draft.accountId)
        assertEquals(cash.id, result.draft.toAccountId)
        assertEquals("Sent Transfer", result.draft.sentLegType)
        assertEquals("Received Transfer", result.draft.receivedLegType)
        assertEquals(true, result.draft.editModeOriginalIsTransfer)

        val original = result.original as EditingOriginal.Transfer
        assertEquals("group-1", original.transferGroup)
        assertEquals("1", original.debitId)
        assertEquals("2", original.creditId)
    }

    @Test
    fun `tapping the credit leg still resolves debit-received roles correctly`() {
        val debit = entry("1", accountId = domestic.id, direction = TxnDirection.Debit, transferGroup = "group-1")
        val credit = entry("2", accountId = cash.id, direction = TxnDirection.Credit, transferGroup = "group-1")

        // Tapped entry is the CREDIT leg this time, sibling is the debit leg.
        val result = buildEditDraft(credit, sibling = debit, accounts = accounts)

        assertEquals(domestic.id, result.draft.accountId) // still the debit leg's account
        assertEquals(cash.id, result.draft.toAccountId) // still the credit leg's account
        val original = result.original as EditingOriginal.Transfer
        assertEquals("1", original.debitId)
        assertEquals("2", original.creditId)
    }

    @Test
    fun `the amount round-trips as a clean, unformatted string`() {
        val e = entry("1")
        val result = buildEditDraft(e, sibling = null, accounts = accounts)
        assertEquals("4500", result.draft.amount.clean)
    }
}
