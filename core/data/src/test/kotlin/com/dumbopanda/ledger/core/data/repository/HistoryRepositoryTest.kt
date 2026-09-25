package com.dumbopanda.ledger.core.data.repository

import androidx.paging.testing.asSnapshot
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

    @Test
    fun `a freshly written offline entry appears in the paged snapshot instantly`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 24)))

        val snapshot = pagedEntries { db }.asSnapshot()

        assertEquals(1, snapshot.size)
        assertEquals("Entry 01", snapshot.single().expenseName)
    }

    @Test
    fun `entries come back newest-date-first`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 20)))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 24)))
        db.entryDao().upsert(entity("03", LocalDate.of(2026, 9, 22)))

        val snapshot = pagedEntries { db }.asSnapshot()

        assertEquals(listOf("02", "03", "01"), snapshot.map { it.id })
    }

    @Test
    fun `a soft-deleted entry never appears in the page`() = runTest {
        db.entryDao().upsert(entity("01", LocalDate.of(2026, 9, 24), deletedAt = Instant.parse("2026-09-24T01:00:00Z")))
        db.entryDao().upsert(entity("02", LocalDate.of(2026, 9, 24)))

        val snapshot = pagedEntries { db }.asSnapshot()

        assertEquals(listOf("02"), snapshot.map { it.id })
    }

    @Test
    fun `an empty history is an empty snapshot, not an error`() = runTest {
        val snapshot = pagedEntries { db }.asSnapshot()
        assertEquals(emptyList<String>(), snapshot.map { it.id })
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
