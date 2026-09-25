package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DeleteConfirmMessageTest {
    private val date = LocalDate.of(2026, 9, 14)

    @Test
    fun `a plain entry gets the standard warning`() {
        val message = buildDeleteConfirmMessage(Money(450_000), "Rapido", date, isTransferLeg = false)
        assertEquals("Delete this entry? ₹4,500 · Rapido · 14 Sep 2026. This cannot be undone.", message)
    }

    @Test
    fun `a transfer leg warns both rows will go, even if it also has an open id`() {
        val message = buildDeleteConfirmMessage(
            Money(1_000_000),
            "Self Transfer",
            date,
            isTransferLeg = true,
            fromAccountName = "Domestic",
            toAccountName = "Cash",
            openTxnId = TxnId("Yabc123"),
        )
        assertEquals("This is one half of a transfer. Deleting it deletes both rows. ₹10,000 Domestic → Cash.", message)
    }

    @Test
    fun `a non-transfer entry with an open id warns about Owed to me`() {
        val message = buildDeleteConfirmMessage(Money(450_000), "Loan to Sam", date, isTransferLeg = false, openTxnId = TxnId("Y3k9d2x"))
        assertEquals("This entry has an open ID (Y3k9d2x). Deleting it removes it from Owed to me.", message)
    }
}
