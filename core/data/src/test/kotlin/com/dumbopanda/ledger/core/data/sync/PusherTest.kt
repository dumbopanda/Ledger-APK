package com.dumbopanda.ledger.core.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import com.dumbopanda.ledger.core.network.sync.SyncResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PusherTest {
    private lateinit var db: LedgerDatabase
    private lateinit var fakeSyncApi: FakeSyncApi
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var pusher: Pusher

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeSyncApi = FakeSyncApi()
        authRepository = FakeAuthRepository()
        pusher = Pusher(
            db.pendingOpDao(),
            db.deadLetterDao(),
            db.entryDao(),
            db.accountDao(),
            fakeSyncApi,
            authRepository,
            LedgerClock { Instant.EPOCH },
        )
    }

    @After
    fun tearDown() = db.close()

    private fun entryPayload(clientUid: String) = Json.encodeToString(
        EntryPushRow(
            clientUid = clientUid,
            entryDate = "2026-09-01",
            txnId = "Nabc123",
            accountId = "account-1",
            expenseName = "Groceries",
            txnType = "Food",
            txnFreq = "one_off",
            amountMinor = 1000,
            direction = "debit",
        ),
    )

    private suspend fun seedInserts(count: Int, entityType: String = "entry", clientUidPrefix: String = "client") {
        repeat(count) { i ->
            db.pendingOpDao().insert(
                PendingOpEntity(
                    entityType = entityType,
                    entityClientUid = "$clientUidPrefix-$i",
                    op = "insert",
                    payloadJson = entryPayload("$clientUidPrefix-$i"),
                    createdAt = Instant.EPOCH,
                ),
            )
        }
    }

    @Test
    fun `250 ops push in 3 calls and drain the queue`() = runTest {
        seedInserts(250)

        val outcome = pusher.push()

        assertEquals(PushOutcome.SUCCESS, outcome)
        assertEquals(3, fakeSyncApi.pushEntriesCallCount)
        assertTrue(db.pendingOpDao().page(1000).isEmpty())
    }

    @Test
    fun `failure on the second call leaves that page and the next queued`() = runTest {
        seedInserts(250) // pages of 100, 100, 50 -> 3 calls
        // Fail exactly the second call with a retryable server error.
        val originalApi = fakeSyncApi
        val outcome = run {
            var seenCalls = 0
            val wrapped = object : com.dumbopanda.ledger.core.network.sync.SyncApi by originalApi {
                override suspend fun pushEntries(rows: List<EntryPushRow>) =
                    if (++seenCalls == 2) SyncResult.ServerError("boom") else originalApi.pushEntries(rows)
            }
            Pusher(db.pendingOpDao(), db.deadLetterDao(), db.entryDao(), db.accountDao(), wrapped, authRepository, LedgerClock { Instant.EPOCH }).push()
        }

        assertEquals(PushOutcome.RETRYABLE, outcome)
        // First page (100 ops) succeeded and was removed; 150 remain queued.
        assertEquals(150, db.pendingOpDao().page(1000).size)
    }

    @Test
    fun `4xx dead-letters a single op and never retries it`() = runTest {
        seedInserts(1)
        fakeSyncApi.entryValidator = { "invalid txn_id" }

        val outcome = pusher.push()

        assertEquals(PushOutcome.SUCCESS, outcome)
        assertTrue(db.pendingOpDao().page(10).isEmpty())
        assertEquals(1, db.deadLetterDao().observeAll().first().size)
        // Re-running push must not touch it again (nothing left to touch).
        val secondOutcome = pusher.push()
        assertEquals(PushOutcome.SUCCESS, secondOutcome)
        assertEquals(1, fakeSyncApi.pushEntriesCallCount) // only ever attempted once
    }

    @Test
    fun `a single bad op in a batch is isolated, dead-lettered, and its batch-mates still push`() = runTest {
        seedInserts(3, clientUidPrefix = "ok-or-bad")
        fakeSyncApi.entryValidator = { row -> if (row.clientUid == "ok-or-bad-1") "invalid" else null }

        val outcome = pusher.push()

        assertEquals(PushOutcome.SUCCESS, outcome)
        assertTrue(db.pendingOpDao().page(10).isEmpty())
    }

    @Test
    fun `401 refreshes once and retries, succeeding on the retry`() = runTest {
        seedInserts(1)
        fakeSyncApi.nextPushResult = SyncResult.Unauthorized

        val outcome = pusher.push()

        assertEquals(PushOutcome.SUCCESS, outcome)
        assertEquals(1, authRepository.refreshCallCount)
        assertTrue(db.pendingOpDao().page(10).isEmpty())
    }

    @Test
    fun `401 that survives a refresh ends the push as session expired`() = runTest {
        seedInserts(1)
        authRepository.refreshSucceeds = false
        fakeSyncApi.nextPushResult = SyncResult.Unauthorized

        val outcome = pusher.push()

        assertEquals(PushOutcome.SESSION_EXPIRED, outcome)
        assertEquals(1, authRepository.refreshCallCount)
        // The op is left queued, not lost.
        assertEquals(1, db.pendingOpDao().page(10).size)
    }
}
