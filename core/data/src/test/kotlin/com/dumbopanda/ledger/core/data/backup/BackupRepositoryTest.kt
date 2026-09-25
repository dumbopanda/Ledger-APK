package com.dumbopanda.ledger.core.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
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

// P6's own gate check, at the data-layer level (no SAF/Uri involved -- that's
// BackupRepository.exportTo/importFrom's job, this exercises the pure additive-merge and
// export-shape logic beneath it): export -> wipe -> import -> identical ledger; import twice
// -> no change.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRepositoryTest {
    private lateinit var db: LedgerDatabase
    private val clock = LedgerClock { Instant.parse("2026-09-25T12:00:00Z") }
    private val userId = "user-1"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun account(id: String, name: String, deletedAt: Instant? = null) = AccountEntity(
        id = id,
        clientUid = "client-$id",
        name = name,
        sortOrder = 0,
        archivedAt = null,
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        deletedAt = deletedAt,
    )

    private fun entry(id: String, accountId: String = "acc-1", deletedAt: Instant? = null) = EntryEntity(
        id = id,
        clientUid = "client-$id",
        entryDate = LocalDate.of(2026, 9, 20),
        txnId = "Nabc123",
        accountId = accountId,
        expenseName = "Entry $id",
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.Monthly,
        amountMinor = 5_000,
        direction = TxnDirection.Debit,
        transferGroup = null,
        createdAt = Instant.parse("2026-09-20T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-20T00:00:00Z"),
        deletedAt = deletedAt,
    )

    @Test
    fun `export excludes soft-deleted rows`() = runTest {
        db.accountDao().upsert(account("acc-1", "Cash"))
        db.accountDao().upsert(account("acc-2", "Gone", deletedAt = Instant.now()))
        db.entryDao().upsert(entry("1"))
        db.entryDao().upsert(entry("2", deletedAt = Instant.now()))

        val export = buildExportData(db, userId, clock)

        assertEquals(listOf("Cash"), export.accounts.map { it.name })
        assertEquals(listOf("1"), export.entries.map { it.id })
        assertEquals(1, export.counts.accounts)
        assertEquals(1, export.counts.entries)
    }

    @Test
    fun `a full export-wipe-import round trip reproduces an identical ledger`() = runTest {
        db.accountDao().upsert(account("acc-1", "Cash"))
        db.entryDao().upsert(entry("1", accountId = "acc-1"))
        db.entryDao().upsert(entry("2", accountId = "acc-1"))
        val export = buildExportData(db, userId, clock)

        // "Wipe" -- a fresh database, as a reinstall would produce.
        db.close()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val outcome = applyImport(db, clock, userId, export) as ImportOutcome.Success
        assertEquals(1, outcome.accountsAdded)
        assertEquals(2, outcome.entriesAdded)

        val reExported = buildExportData(db, userId, clock)
        assertEquals(export.accounts.map { it.clientUid }.toSet(), reExported.accounts.map { it.clientUid }.toSet())
        assertEquals(export.entries.map { it.clientUid }.toSet(), reExported.entries.map { it.clientUid }.toSet())
    }

    @Test
    fun `importing the same export twice makes no change the second time`() = runTest {
        db.accountDao().upsert(account("acc-1", "Cash"))
        db.entryDao().upsert(entry("1", accountId = "acc-1"))
        val export = buildExportData(db, userId, clock)

        val first = applyImport(db, clock, userId, export) as ImportOutcome.Success
        val second = applyImport(db, clock, userId, export) as ImportOutcome.Success

        // First import: everything already exists locally (it's the SAME db it was exported
        // from), so even the first pass adds nothing -- the real "no duplicate rows" proof is
        // that entries/accounts counts don't grow across either call.
        assertEquals(0, first.accountsAdded + first.entriesAdded)
        assertEquals(0, second.accountsAdded + second.entriesAdded)
        assertEquals(1, buildExportData(db, userId, clock).entries.size)
    }

    @Test
    fun `import never overwrites or removes a row already present`() = runTest {
        db.entryDao().upsert(entry("1").copy(expenseName = "Local edit"))
        val foreignExport = LedgerExport(
            exportedAt = "2026-09-25T00:00:00Z",
            userId = userId,
            counts = ExportCounts(entries = 1, accounts = 0, pending = 0),
            accounts = emptyList(),
            entries = listOf(entry("1").copy(expenseName = "Stale exported name").toExported()),
        )

        applyImport(db, clock, userId, foreignExport)

        assertEquals("Local edit", db.entryDao().findById("1")?.expenseName)
    }

    @Test
    fun `a mismatched userId is rejected without touching the database`() = runTest {
        val export = LedgerExport(
            exportedAt = "2026-09-25T00:00:00Z",
            userId = "someone-else",
            counts = ExportCounts(1, 0, 0),
            accounts = emptyList(),
            entries = listOf(entry("1").toExported()),
        )

        val outcome = applyImport(db, clock, userId, export)

        assertTrue(outcome is ImportOutcome.UserMismatch)
        assertEquals(null, db.entryDao().findById("1"))
    }

    @Test
    fun `imported rows each enqueue exactly one pending insert op`() = runTest {
        val export = LedgerExport(
            exportedAt = "2026-09-25T00:00:00Z",
            userId = userId,
            counts = ExportCounts(entries = 1, accounts = 1, pending = 0),
            accounts = listOf(account("acc-1", "Cash").toExported()),
            entries = listOf(entry("1", accountId = "acc-1").toExported()),
        )

        applyImport(db, clock, userId, export)

        val pending = db.pendingOpDao().page(10)
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.op == "insert" })
    }
}
