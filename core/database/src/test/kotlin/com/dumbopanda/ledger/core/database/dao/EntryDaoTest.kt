package com.dumbopanda.ledger.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EntryDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: EntryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.entryDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entry(
        id: String = java.util.UUID.randomUUID().toString(),
        clientUid: String = "client-$id",
        entryDate: LocalDate = LocalDate.of(2026, 9, 1),
        txnId: String = "Nabc123",
        accountId: String = "account-1",
        amountMinor: Long = 10_000,
        direction: TxnDirection = TxnDirection.Debit,
        transferGroup: String? = null,
        createdAt: Instant = Instant.parse("2026-09-01T00:00:00Z"),
        deletedAt: Instant? = null,
    ) = EntryEntity(
        id = id,
        clientUid = clientUid,
        entryDate = entryDate,
        txnId = txnId,
        accountId = accountId,
        expenseName = "Groceries",
        moreInfo = null,
        txnType = "Food",
        txnFreq = TxnFrequency.OneOff,
        amountMinor = amountMinor,
        direction = direction,
        transferGroup = transferGroup,
        createdAt = createdAt,
        updatedAt = createdAt,
        deletedAt = deletedAt,
    )

    @Test
    fun `pageByDate orders newest first and excludes deleted`() = runTest {
        dao.upsert(entry(id = "1", entryDate = LocalDate.of(2026, 9, 1)))
        dao.upsert(entry(id = "2", entryDate = LocalDate.of(2026, 9, 3)))
        dao.upsert(entry(id = "3", entryDate = LocalDate.of(2026, 9, 2), deletedAt = Instant.now()))

        val page = dao.pageByDate(limit = 10, offset = 0).first()

        assertEquals(listOf("2", "1"), page.map { it.id })
    }

    @Test
    fun `pageByDate respects limit and offset`() = runTest {
        repeat(5) { i ->
            dao.upsert(entry(id = "e$i", entryDate = LocalDate.of(2026, 9, 1).plusDays(i.toLong())))
        }

        val firstPage = dao.pageByDate(limit = 2, offset = 0).first()
        val secondPage = dao.pageByDate(limit = 2, offset = 2).first()

        assertEquals(listOf("e4", "e3"), firstPage.map { it.id })
        assertEquals(listOf("e2", "e1"), secondPage.map { it.id })
    }

    @Test
    fun `openYIds is empty when there is no Y entry`() = runTest {
        dao.upsert(entry(id = "1", txnId = "Nabc123"))

        assertTrue(dao.openYIds().first().isEmpty())
    }

    @Test
    fun `openYIds surfaces a Y-id that has appeared an odd number of times`() = runTest {
        dao.upsert(entry(id = "1", txnId = "Yabc123"))

        assertEquals(listOf("Yabc123"), dao.openYIds().first())
    }

    @Test
    fun `openYIds closes once the same Y-id appears a second time`() = runTest {
        dao.upsert(entry(id = "1", txnId = "Yabc123"))
        dao.upsert(entry(id = "2", txnId = "Yabc123"))

        assertTrue(dao.openYIds().first().isEmpty())
    }

    @Test
    fun `openYIds reopens once the returning leg is soft-deleted`() = runTest {
        dao.upsert(entry(id = "1", txnId = "Yabc123"))
        dao.upsert(entry(id = "2", txnId = "Yabc123"))
        dao.upsert(entry(id = "2", txnId = "Yabc123", deletedAt = Instant.now()))

        assertEquals(listOf("Yabc123"), dao.openYIds().first())
    }

    @Test
    fun `transferLegs returns both legs of a group and nothing else`() = runTest {
        dao.upsertAll(
            entry(id = "out", transferGroup = "group-1", direction = TxnDirection.Debit),
            entry(id = "in", transferGroup = "group-1", direction = TxnDirection.Credit),
            entry(id = "unrelated", transferGroup = null),
        )

        val legs = dao.transferLegs("group-1")

        assertEquals(setOf("out", "in"), legs.map { it.id }.toSet())
    }

    @Test
    fun `netMinorBetween sums credit minus debit and excludes transfers`() = runTest {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 30)
        dao.upsert(entry(id = "expense", entryDate = from, amountMinor = 5_000, direction = TxnDirection.Debit))
        dao.upsert(entry(id = "income", entryDate = from.plusDays(1), amountMinor = 20_000, direction = TxnDirection.Credit))
        dao.upsert(
            entry(
                id = "transfer-out",
                entryDate = from.plusDays(2),
                amountMinor = 100_000,
                direction = TxnDirection.Debit,
                transferGroup = "group-1",
            ),
        )
        dao.upsert(entry(id = "outside-range", entryDate = to.plusDays(1), amountMinor = 999_999, direction = TxnDirection.Credit))

        val net = dao.netMinorBetween(from, to).first()

        assertEquals(20_000L - 5_000L, net)
    }
}
