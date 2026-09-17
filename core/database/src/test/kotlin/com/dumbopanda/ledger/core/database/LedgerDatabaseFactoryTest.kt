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
}
