package com.dumbopanda.ledger.core.database

import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

// The actual claim from §6.4: "Two accounts on one phone cannot see each
// other's data because they are not in the same file." Proven here by
// opening two real (non-in-memory) database files under the same app
// context and confirming a row written under one userId's file is genuinely
// absent — not just filtered — from the other.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LedgerDatabaseFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val factory = LedgerDatabaseFactory(context)

    @Test
    fun `same userId always maps to the same file name`() {
        assertEquals(LedgerDatabaseFactory.fileNameFor("user-a"), LedgerDatabaseFactory.fileNameFor("user-a"))
    }

    @Test
    fun `different userIds map to different file names`() {
        assertNotEquals(LedgerDatabaseFactory.fileNameFor("user-a"), LedgerDatabaseFactory.fileNameFor("user-b"))
    }

    @Test
    fun `a row written for user A is genuinely absent from user B's database`() = runTest {
        val dbA = factory.open("user-a")
        dbA.accountDao().upsert(
            AccountEntity(
                id = "acc-1",
                clientUid = "client-1",
                name = "Cash",
                sortOrder = 0,
                archivedAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                deletedAt = null,
            ),
        )
        dbA.close()

        val dbB = factory.open("user-b")
        val found = dbB.accountDao().findByClientUid("client-1")
        dbB.close()

        assertNull(found)
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-a"))
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-b"))
    }

    // C10's own gate, at the file level: opening user B's database in between does not disturb
    // user A's file -- reopening it afterwards reads back exactly what was written, not a
    // fresh/wiped file. SessionDatabaseHolderTest (core:data) proves the same claim through the
    // app's actual reopen-on-user-switch seam; this proves the lower-level guarantee it relies
    // on -- a closed Room file genuinely persists to disk rather than being an in-memory
    // artifact of the connection that wrote it.
    @Test
    fun `user A's data survives a visit to user B's database in between`() = runTest {
        val dbA1 = factory.open("user-a")
        dbA1.accountDao().upsert(
            AccountEntity(
                id = "acc-1",
                clientUid = "client-1",
                name = "Cash",
                sortOrder = 0,
                archivedAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                deletedAt = null,
            ),
        )
        dbA1.close()

        val dbB = factory.open("user-b")
        dbB.close()

        val dbA2 = factory.open("user-a")
        val restored = dbA2.accountDao().findByClientUid("client-1")
        dbA2.close()

        assertEquals("Cash", restored?.name)
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-a"))
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-b"))
    }
}
