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
}
