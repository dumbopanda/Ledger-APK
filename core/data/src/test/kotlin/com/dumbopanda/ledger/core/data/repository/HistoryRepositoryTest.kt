package com.dumbopanda.ledger.core.data.repository

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryRepositoryTest {
    private lateinit var db: LedgerDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(id: String, date: LocalDate, deletedAt: Instant? = null, txnId: String = "Nabc123") = EntryEntity(
        id = id,
        clientUid = "client-$id",
        entryDate = date,
        txnId = txnId,
        accountId = "account-1",
        expenseName = "Entry $id",
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.OneOff,
        amountMinor = 1_000,
        direction = TxnDirection.Debit,
        transferGroup = null,
        createdAt = Instant.parse("2026-09-${id.padStart(2, '0')}T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-${id.padStart(2, '0')}T00:00:00Z"),
        deletedAt = deletedAt,
    )

    private val september = YearMonth.of(2026, 9)

    @Test
    fun `a freshly written offline entry appears in its month instantly`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 24)))

        val month = entriesInMonth(september) { db }.first()

        assertEquals(1, month.size)
        assertEquals("Entry 01", month.single().expenseName)
    }

    @Test
    fun `entries come back newest-date-first`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 20)))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 24)))
        db.entryDao().upsert(entity("03", LocalDate.of(2026, 9, 22)))

        val month = entriesInMonth(september) { db }.first()

        assertEquals(listOf("02", "03", "01"), month.map { it.id })
    }

    @Test
    fun `a soft-deleted entry never appears`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 24), deletedAt = Instant.parse("2026-09-24T01:00:00Z")))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 24)))

        val month = entriesInMonth(september) { db }.first()

        assertEquals(listOf("02"), month.map { it.id })
    }

    @Test
    fun `only the asked-for month comes back, both of its edge days included`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 8, 31)))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 1)))
        db.entryDao().upsert(entity("03", LocalDate.of(2026, 9, 30)))
        db.entryDao().upsert(entity("04", LocalDate.of(2026, 10, 1)))

        val month = entriesInMonth(september) { db }.first()

        assertEquals(listOf("03", "02"), month.map { it.id })
    }

    @Test
    fun `an empty month is an empty list, not an error`() = runTest {
        assertEquals(emptyList<String>(), entriesInMonth(september) { db }.first().map { it.id })
    }

    @Test
    fun `owedToMeEntries returns only live Y-prefixed entries`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 20), txnId = "Yabc123"))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 21), txnId = "Nabc123")) // not a Y entry
        db.entryDao().upsert(
            entity("03", LocalDate.of(2026, 9, 22), txnId = "Ydef456", deletedAt = Instant.parse("2026-09-22T01:00:00Z")),
        )

        val result = owedToMeEntries { db }.first()

        assertEquals(listOf("01"), result.map { it.id })
    }
}
