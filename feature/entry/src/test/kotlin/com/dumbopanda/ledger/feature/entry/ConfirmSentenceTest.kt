package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConfirmSentenceTest {
    private val domestic = Account("1", "client-1-uid", "Domestic", 0, null)
    private val cash = Account("2", "client-2-uid", "Cash", 1, null)
    private val date = LocalDate.of(2026, 9, 14)

    @Test
    fun `a debit reads as money leaving the account`() {
        val sentence = buildConfirmSentence(
            amount = AmountFieldValue("4500", 4),
            direction = TxnDirection.Debit,
            date = date,
            expenseName = "Rapido",
            account = domestic,
            selfTransfer = false,
            toAccount = cash,
        )
        assertEquals("₹4,500 left your Domestic account on Monday 14 Sep 2026 for Rapido.", sentence)
    }

    @Test
    fun `a credit reads as money coming in`() {
        val sentence = buildConfirmSentence(
            amount = AmountFieldValue("4500", 4),
            direction = TxnDirection.Credit,
            date = date,
            expenseName = "Salary",
            account = domestic,
            selfTransfer = false,
            toAccount = cash,
        )
        assertEquals("₹4,500 came into your Domestic account on Monday 14 Sep 2026 for Salary.", sentence)
    }

    @Test
    fun `a self transfer reads as money moving between the two accounts`() {
        val sentence = buildConfirmSentence(
            amount = AmountFieldValue("10000", 5),
            direction = TxnDirection.Debit,
            date = date,
            expenseName = "Self Transfer",
            account = domestic,
            selfTransfer = true,
            toAccount = cash,
        )
        assertEquals("₹10,000 moved from your Domestic account to your Cash account on Monday 14 Sep 2026.", sentence)
    }
}
