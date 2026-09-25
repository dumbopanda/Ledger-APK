package com.dumbopanda.ledger.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Account
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountRepositoryTest {
    private lateinit var db: LedgerDatabase
    private val clock = LedgerClock { Instant.parse("2026-09-25T00:00:00Z") }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun account(id: String, name: String) = Account(id, "client-$id", name, sortOrder = 0, archivedAt = null)

    @Test
    fun `a freshly written account is queued as a pending op AND appears in activeAccounts`() = runTest {
        writeAccount(db, clock, account("1", "Domestic"))

        assertEquals(listOf("Domestic"), activeAccounts { db }.first().map { it.name })
        assertEquals(1, db.pendingOpDao().page(10).size)
    }

    @Test
    fun `activeAccounts is empty with no accounts, not an error`() = runTest {
        assertEquals(emptyList<Account>(), activeAccounts { db }.first())
    }

    // §7.12 / C6 -------------------------------------------------------------------------------

    @Test
    fun `renameAccount updates the name and enqueues an update op`() = runTest {
        writeAccount(db, clock, account("1", "Cash"))
        db.pendingOpDao().page(10).forEach { db.pendingOpDao().deleteBySeq(it.seq) } // simulate: already pushed

        renameAccount(db, clock, "1", "Wallet")

        assertEquals("Wallet", db.accountDao().findById("1")?.name)
        val pending = db.pendingOpDao().page(10)
        assertEquals(1, pending.size)
        assertEquals("update", pending.single().op)
    }

    @Test
    fun `setArchived archives then unarchives`() = runTest {
        writeAccount(db, clock, account("1", "Cash"))

        setArchived(db, clock, "1", archived = true)
        assertTrueArchived(db, "1")

        setArchived(db, clock, "1", archived = false)
        assertEquals(null, db.accountDao().findById("1")?.archivedAt)
    }

    private suspend fun assertTrueArchived(db: LedgerDatabase, id: String) {
        assertEquals(true, db.accountDao().findById(id)?.archivedAt != null)
    }

    @Test
    fun `reorder writes sortOrder in list order and enqueues one op per account`() = runTest {
        writeAccount(db, clock, account("1", "Cash"))
        writeAccount(db, clock, account("2", "Domestic"))
        db.pendingOpDao().page(10).forEach { db.pendingOpDao().deleteBySeq(it.seq) }

        reorder(db, clock, listOf("2", "1"))

        assertEquals(0, db.accountDao().findById("2")?.sortOrder)
        assertEquals(1, db.accountDao().findById("1")?.sortOrder)
        assertEquals(2, db.pendingOpDao().page(10).size)
    }

    @Test
    fun `ensureSeeded seeds Domestic, International, Cash when empty`() = runTest {
        ensureSeeded(db, clock)

        val all = allAccounts { db }.first()
        assertEquals(listOf("Domestic", "International", "Cash"), all.map { it.name })
        assertEquals(3, db.pendingOpDao().page(10).size)
    }

    @Test
    fun `ensureSeeded does nothing when an account already exists`() = runTest {
        writeAccount(db, clock, account("1", "My Own Account"))
        db.pendingOpDao().page(10).forEach { db.pendingOpDao().deleteBySeq(it.seq) }

        ensureSeeded(db, clock)

        assertEquals(listOf("My Own Account"), allAccounts { db }.first().map { it.name })
        assertEquals(0, db.pendingOpDao().page(10).size)
    }
}
