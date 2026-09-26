package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

// The confirm sheet's sentences are index.html's own, word for word.
class ConfirmSentenceTest {
    private val domestic = Account("1", "client-1-uid", "Domestic", 0, null)
    private val cash = Account("2", "client-2-uid", "Cash", 1, null)

    private fun draft(
        amount: String = "4500",
        direction: TxnDirection = TxnDirection.Debit,
        name: String = "Rapido",
        selfTransfer: Boolean = false,
        txnId: String = "Nabc123",
        note: String? = null,
    ) = EntryDraft(
        resolvedDate = LocalDate.of(2026, 9, 14),
        expenseName = name,
        moreInfo = note,
        selfTransfer = selfTransfer,
        accountId = domestic.id,
        toAccountId = cash.id,
        txnId = TxnId(txnId),
        txnType = "Payment",
        sentLegType = "Sent Transfer",
        receivedLegType = "Received Transfer",
        amount = AmountFieldValue(amount, amount.length),
        direction = direction,
        editModeOriginalIsTransfer = null,
    )

    @Test
    fun `a one-way debit reads as money leaving, nothing expected back`() {
        val sheet = buildConfirmSheet(draft(), domestic, cash, backOld = false, isEditing = false)
        assertEquals(
            "₹4,500.00 leaves your Domestic account on 14 Sept 2026 for Rapido. It is a one-way payment, so nothing is expected back.",
            sheet.line,
        )
        assertEquals("Add this entry?", sheet.title)
        assertEquals("Edit entry", sheet.backLabel)
        assertEquals("Add entry", sheet.confirmLabel)
        assertNull(sheet.note)
    }

    @Test
    fun `a credit reads as money coming in from the name`() {
        val sheet = buildConfirmSheet(draft(direction = TxnDirection.Credit, name = "Salary"), domestic, cash, backOld = false, isEditing = false)
        assertEquals(
            "₹4,500.00 comes into your Domestic account on 14 Sept 2026 from Salary. It is a one-way payment, so nothing is expected back.",
            sheet.line,
        )
    }

    @Test
    fun `a new Y stays under Owed to me, an old Y closes one`() {
        val new = buildConfirmSheet(draft(txnId = "Yabc123"), domestic, cash, backOld = false, isEditing = false)
        assertEquals(true, new.line.endsWith("It is marked to be returned, so it stays under Owed to me until the other half is entered."))
        assertEquals("Yes · a new one", new.rows.first { it.first == "Will I get this back" }.second)

        val old = buildConfirmSheet(draft(txnId = "Yabc123"), domestic, cash, backOld = true, isEditing = false)
        assertEquals(true, old.line.endsWith("It closes an open item, so it drops off Owed to me once it is saved."))
        assertEquals("Yes · closing an open one", old.rows.first { it.first == "Will I get this back" }.second)
    }

    @Test
    fun `a self transfer reads as money moving between the two accounts`() {
        val sheet = buildConfirmSheet(draft(amount = "10000", selfTransfer = true, name = "Self Transfer", txnId = "Yabc123"), domestic, cash, backOld = false, isEditing = false)
        assertEquals(
            "₹10,000.00 moves from your Domestic account to your Cash account on 14 Sept 2026. " +
                "It is your own money changing hands, so it is recorded as two rows but counts as neither spending nor income.",
            sheet.line,
        )
        assertEquals("₹10,000.00 moved", sheet.rows.first { it.first == "Amount" }.second)
        assertEquals("14 Sept 2026 (Monday)", sheet.rows.first { it.first == "Date" }.second)
    }

    @Test
    fun `a lakh or more gets the check-the-zeros note, and editing relabels everything`() {
        val sheet = buildConfirmSheet(draft(amount = "123456"), domestic, cash, backOld = false, isEditing = true)
        assertEquals("That is 1.23 lakh — check the zeros", sheet.note)
        assertEquals("Save these changes?", sheet.title)
        assertEquals("Keep editing", sheet.backLabel)
        assertEquals("Save changes", sheet.confirmLabel)
    }
}
