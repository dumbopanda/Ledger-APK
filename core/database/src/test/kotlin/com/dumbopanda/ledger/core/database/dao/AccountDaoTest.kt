package com.dumbopanda.ledger.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.AccountEntity
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
class AccountDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: AccountDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.accountDao()
    }

    @After
    fun tearDown() = db.close()

    private fun account(
        id: String,
        name: String,
        sortOrder: Int = 0,
        archivedAt: Instant? = null,
        deletedAt: Instant? = null,
    ) = AccountEntity(
        id = id,
        clientUid = "client-$id",
        name = name,
        sortOrder = sortOrder,
        archivedAt = archivedAt,
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        deletedAt = deletedAt,
    )

    @Test
    fun `observeAll orders by sortOrder then name and excludes deleted`() = runTest {
        dao.upsert(account(id = "1", name = "Cash", sortOrder = 2))
        dao.upsert(account(id = "2", name = "Domestic", sortOrder = 0))
        dao.upsert(account(id = "3", name = "International", sortOrder = 1))
        dao.upsert(account(id = "4", name = "Deleted", sortOrder = -1, deletedAt = Instant.now()))

        val all = dao.observeAll().first()

        assertEquals(listOf("Domestic", "International", "Cash"), all.map { it.name })
    }

    @Test
    fun `observeActive excludes archived but observeAll includes it`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))
        dao.upsert(account(id = "2", name = "Old Wallet", archivedAt = Instant.now()))

        val active = dao.observeActive().first()
        val all = dao.observeAll().first()

        assertEquals(listOf("Cash"), active.map { it.name })
        assertEquals(setOf("Cash", "Old Wallet"), all.map { it.name }.toSet())
    }

    @Test
    fun `findByClientUid finds an upserted account`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))

        val found = dao.findByClientUid("client-1")

        assertEquals("1", found?.id)
    }

    // §7.12 / C6 -------------------------------------------------------------------------------

    @Test
    fun `countLive counts non-deleted rows only`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))
        dao.upsert(account(id = "2", name = "Old", deletedAt = Instant.now()))
        assertEquals(1, dao.countLive())
    }

    @Test
    fun `countByNameExcluding is case-insensitive and excludes the account itself`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))

        assertEquals(1, dao.countByNameExcluding("cash", excludingId = "other-id"))
        assertEquals(0, dao.countByNameExcluding("cash", excludingId = "1"))
        assertEquals(0, dao.countByNameExcluding("Wallet", excludingId = "other-id"))
    }

    @Test
    fun `rename updates the name`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))
        dao.rename("1", "Wallet", Instant.parse("2026-09-25T00:00:00Z"))
        assertEquals("Wallet", dao.findById("1")?.name)
    }

    @Test
    fun `setArchived sets and clears archivedAt`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))
        val now = Instant.parse("2026-09-25T00:00:00Z")

        dao.setArchived("1", now, now)
        assertEquals(now, dao.findById("1")?.archivedAt)

        dao.setArchived("1", null, now)
        assertEquals(null, dao.findById("1")?.archivedAt)
    }

    @Test
    fun `updateSortOrder moves an account`() = runTest {
        dao.upsert(account(id = "1", name = "Cash", sortOrder = 0))
        dao.updateSortOrder("1", 5, Instant.now())
        assertEquals(5, dao.findById("1")?.sortOrder)
    }

    @Test
    fun `allLive includes archived but excludes deleted`() = runTest {
        dao.upsert(account(id = "1", name = "Cash"))
        dao.upsert(account(id = "2", name = "Old Wallet", archivedAt = Instant.now()))
        dao.upsert(account(id = "3", name = "Gone", deletedAt = Instant.now()))

        val all = dao.allLive()

        assertEquals(setOf("Cash", "Old Wallet"), all.map { it.name }.toSet())
    }
}
