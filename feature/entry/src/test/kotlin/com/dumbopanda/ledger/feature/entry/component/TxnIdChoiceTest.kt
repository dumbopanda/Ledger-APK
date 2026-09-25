package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TxnIdChoiceTest {
    private val fresh = TxnId("Yfresh1")

    private fun openItem(id: String, name: String = "Loan to Sam", amount: Long = 50_000) = OwedToMeItem(
        txnId = TxnId(id),
        isOpen = true,
        expenseName = name,
        amount = Money(amount),
        accountId = "acc-1",
        openedDate = LocalDate.of(2026, 9, 10),
        closedDate = null,
        openLegEntryId = "entry-$id",
        closeLegEntryId = null,
    )

    @Test
    fun `no open items -- just the fresh option`() {
        val result = buildTxnIdChoices(TxnId("Nabcdef"), openItems = emptyList(), freshId = fresh)
        assertEquals(listOf(TxnIdChoice.Fresh(fresh)), result)
    }

    @Test
    fun `open items appear as Existing choices carrying name, amount and date`() {
        val item = openItem("Yopen01")
        val result = buildTxnIdChoices(fresh, openItems = listOf(item), freshId = fresh)
        assertEquals(
            listOf(TxnIdChoice.Fresh(fresh), TxnIdChoice.Existing(item.txnId, item.expenseName, item.amount, item.openedDate)),
            result,
        )
    }

    @Test
    fun `edit mode -- a closed stored id falls back to the entry's own known fields`() {
        val stored = TxnId("Yclosed")
        val fallback = TxnIdChoice.Existing(stored, "Old Loan", Money(20_000), LocalDate.of(2026, 8, 1))
        val result = buildTxnIdChoices(stored, openItems = emptyList(), freshId = fresh, editFallback = fallback)
        assertEquals(listOf(fallback, TxnIdChoice.Fresh(fresh)), result)
    }

    @Test
    fun `edit mode -- a stored id that IS still open uses the live item, not the fallback`() {
        val item = openItem("Yopen01")
        val staleFallback = TxnIdChoice.Existing(item.txnId, "Stale name", Money(1), LocalDate.of(2020, 1, 1))
        val result = buildTxnIdChoices(item.txnId, openItems = listOf(item), freshId = fresh, editFallback = staleFallback)
        assertTrue(result.contains(TxnIdChoice.Existing(item.txnId, item.expenseName, item.amount, item.openedDate)))
        assertTrue(!result.contains(staleFallback))
    }
}
