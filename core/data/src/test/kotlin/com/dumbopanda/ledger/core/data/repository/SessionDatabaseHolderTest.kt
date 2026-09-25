package com.dumbopanda.ledger.core.data.repository

import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import com.dumbopanda.ledger.core.datastore.SessionStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

// C10's own gate: "Sign in as A, then B, then A -> A's data intact." SessionDatabaseHolder is
// the ONE place in the app that reopens a per-user database on a user switch (its own doc
// comment); this drives that exact switch with real, file-backed Room databases (not
// in-memory -- the claim is specifically that closing and reopening a file doesn't lose
// anything) via its internal userId overload (see that method's own doc for why SessionStore
// itself is never touched here).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionDatabaseHolderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val factory = LedgerDatabaseFactory(context)
    // Never read from/written to/cleared -- SessionStore's own `prefs` is lazy specifically so
    // this construction never touches the (Robolectric-unavailable) Android Keystore.
    private val holder = SessionDatabaseHolder(factory, SessionStore(context))

    @After
    fun tearDown() {
        holder.close()
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-a"))
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor("user-b"))
    }

    @Test
    fun `the same userId reuses the already-open connection instead of reopening`() {
        val first = holder.database("user-a")
        val second = holder.database("user-a")
        assertSame(first, second)
    }

    @Test
    fun `switching userId closes the previous connection and opens a fresh one`() {
        val dbA = holder.database("user-a")
        val dbB = holder.database("user-b")
        assertNotSame(dbA, dbB)
    }

    @Test
    fun `sign in as A, then B, then A -- A's data is intact`() = runTest {
        val dbA1 = holder.database("user-a")
        dbA1.accountDao().upsert(account(clientUid = "acc-a", name = "Cash"))

        val dbB = holder.database("user-b")
        dbB.accountDao().upsert(account(clientUid = "acc-b", name = "Wallet"))
        assertNull(dbB.accountDao().findByClientUid("acc-a"))

        val dbA2 = holder.database("user-a")
        // A genuinely fresh connection, not a cached one still pointed at user B's file --
        // the claim is that closing and reopening the FILE preserves the row, not that the
        // in-memory object never changed.
        assertNotSame(dbA1, dbA2)
        val restored = dbA2.accountDao().findByClientUid("acc-a")
        assertEquals("Cash", restored?.name)
    }

    private fun account(clientUid: String, name: String) = AccountEntity(
        id = clientUid,
        clientUid = clientUid,
        name = name,
        sortOrder = 0,
        archivedAt = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
    )
}
