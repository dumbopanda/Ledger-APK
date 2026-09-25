package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GroupOwedToMeUseCaseTest {
    private val useCase = GroupOwedToMeUseCase()

    private fun entry(
        id: String,
        txnId: String,
        date: LocalDate,
        createdAt: Instant = Instant.parse("2026-09-24T00:00:00Z"),
        expenseName: String = "Loan to Sam",
        amountMinor: Long = 50_000,
        accountId: String = "acc-1",
    ) = Entry(
        id = id,
        clientUid = "client-$id",
        entryDate = date,
        txnId = TxnId(txnId),
        accountId = accountId,
        expenseName = expenseName,
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.Monthly,
        amount = Money(amountMinor),
        direction = TxnDirection.Debit,
        transferGroup = null,
        createdAt = createdAt,
    )

    private val d1 = LocalDate.of(2026, 9, 10)
    private val d2 = LocalDate.of(2026, 9, 20)

    @Test
    fun `no Y entries produces no items`() {
        val entries = listOf(entry("1", "Nabc123", d1))
        assertTrue(useCase.group(entries).isEmpty())
    }

    @Test
    fun `a single Y leg is open`() {
        val item = useCase.group(listOf(entry("1", "Yabc123", d1))).single()
        assertTrue(item.isOpen)
        assertEquals(d1, item.openedDate)
        assertNull(item.closedDate)
        assertEquals("1", item.openLegEntryId)
        assertNull(item.closeLegEntryId)
    }

    @Test
    fun `two legs for the same id close the pair`() {
        val open = entry("1", "Yabc123", d1, expenseName = "Loan to Sam")
        val close = entry("2", "Yabc123", d2, expenseName = "Repaid by Sam")
        val item = useCase.group(listOf(open, close)).single()
        assertTrue(!item.isOpen)
        assertEquals(d1, item.openedDate)
        assertEquals(d2, item.closedDate)
        assertEquals("1", item.openLegEntryId)
        assertEquals("2", item.closeLegEntryId)
    }

    @Test
    fun `a closed pair displays the more recent leg's fields`() {
        val open = entry("1", "Yabc123", d1, expenseName = "Loan to Sam", amountMinor = 50_000)
        val close = entry("2", "Yabc123", d2, expenseName = "Repaid by Sam", amountMinor = 50_000)
        val item = useCase.group(listOf(open, close)).single()
        assertEquals("Repaid by Sam", item.expenseName)
    }

    @Test
    fun `input order does not matter, legs are sorted by date`() {
        val open = entry("1", "Yabc123", d1)
        val close = entry("2", "Yabc123", d2)
        val item = useCase.group(listOf(close, open)).single()
        assertEquals(d1, item.openedDate)
        assertEquals(d2, item.closedDate)
    }

    @Test
    fun `different Y ids produce separate items`() {
        val entries = listOf(entry("1", "Yaaa111", d1), entry("2", "Ybbb222", d2))
        val items = useCase.group(entries)
        assertEquals(2, items.size)
        assertTrue(items.all { it.isOpen })
    }

    @Test
    fun `a reopened id -- three legs -- is open again, spanning earliest to latest`() {
        val leg1 = entry("1", "Yabc123", LocalDate.of(2026, 9, 1))
        val leg2 = entry("2", "Yabc123", LocalDate.of(2026, 9, 10))
        val leg3 = entry("3", "Yabc123", LocalDate.of(2026, 9, 20))
        val item = useCase.group(listOf(leg1, leg2, leg3)).single()
        assertTrue(item.isOpen)
        assertEquals(LocalDate.of(2026, 9, 1), item.openedDate)
        assertEquals("1", item.openLegEntryId)
        assertNull(item.closeLegEntryId)
    }
}
