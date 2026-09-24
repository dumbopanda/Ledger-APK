package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GroupEntriesByDayUseCaseTest {
    private val useCase = GroupEntriesByDayUseCase()

    private fun entry(
        id: String,
        date: LocalDate,
        amountMinor: Long,
        direction: TxnDirection,
        transferGroup: String? = null,
    ) = Entry(
        id = id,
        clientUid = "client-$id",
        entryDate = date,
        txnId = TxnId("Nabc123"),
        accountId = "account-1",
        expenseName = "Test",
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.OneOff,
        amount = Money(amountMinor),
        direction = direction,
        transferGroup = transferGroup,
        createdAt = Instant.parse("2026-09-24T00:00:00Z"),
    )

    private val day1 = LocalDate.of(2026, 9, 24)
    private val day2 = LocalDate.of(2026, 9, 23)

    @Test
    fun `an empty list produces no groups`() {
        assertEquals(emptyList<DayGroup>(), useCase.group(emptyList()))
    }

    @Test
    fun `entries on the same day land in one group`() {
        val entries = listOf(
            entry("1", day1, 10_000, TxnDirection.Debit),
            entry("2", day1, 5_000, TxnDirection.Credit),
        )
        val groups = useCase.group(entries)
        assertEquals(1, groups.size)
        assertEquals(day1, groups.single().date)
        assertEquals(2, groups.single().entries.size)
    }

    @Test
    fun `entries on different days land in separate groups, newest day first preserved`() {
        val entries = listOf(entry("1", day1, 1_000, TxnDirection.Debit), entry("2", day2, 1_000, TxnDirection.Debit))
        val groups = useCase.group(entries)
        assertEquals(listOf(day1, day2), groups.map { it.date })
    }

    @Test
    fun `within-day order is preserved from the input, not re-sorted`() {
        val newest = entry("newest", day1, 1_000, TxnDirection.Debit)
        val oldest = entry("oldest", day1, 2_000, TxnDirection.Debit)
        val groups = useCase.group(listOf(newest, oldest))
        assertEquals(listOf("newest", "oldest"), groups.single().entries.map { it.id })
    }

    @Test
    fun `net is debit minus credit -- a pure spend day is negative`() {
        val entries = listOf(entry("1", day1, 10_000, TxnDirection.Debit))
        assertEquals(-10_000L, useCase.group(entries).single().netMinor)
    }

    @Test
    fun `net is positive on a pure income day`() {
        val entries = listOf(entry("1", day1, 10_000, TxnDirection.Credit))
        assertEquals(10_000L, useCase.group(entries).single().netMinor)
    }

    @Test
    fun `net nets debits and credits against each other within the same day`() {
        val entries = listOf(
            entry("1", day1, 10_000, TxnDirection.Debit),
            entry("2", day1, 4_000, TxnDirection.Credit),
        )
        assertEquals(-6_000L, useCase.group(entries).single().netMinor)
    }

    @Test
    fun `a transfer leg is excluded from the day's net -- moving your own money is not spending`() {
        val entries = listOf(
            entry("1", day1, 10_000, TxnDirection.Debit), // real spend
            entry("2", day1, 50_000, TxnDirection.Debit, transferGroup = "group-1"), // transfer leg
            entry("3", day1, 50_000, TxnDirection.Credit, transferGroup = "group-1"), // its sibling
        )
        val group = useCase.group(entries).single()
        assertEquals(3, group.entries.size) // still present in the day, just not in the net
        assertEquals(-10_000L, group.netMinor)
    }

    @Test
    fun `a day with only transfers nets to zero`() {
        val entries = listOf(
            entry("1", day1, 50_000, TxnDirection.Debit, transferGroup = "group-1"),
            entry("2", day1, 50_000, TxnDirection.Credit, transferGroup = "group-1"),
        )
        assertEquals(0L, useCase.group(entries).single().netMinor)
    }
}
