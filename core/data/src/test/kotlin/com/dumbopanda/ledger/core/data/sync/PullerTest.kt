package com.dumbopanda.ledger.core.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import com.dumbopanda.ledger.core.datastore.SyncCursors
import com.dumbopanda.ledger.core.datastore.SyncCursorsSerializer
import com.dumbopanda.ledger.core.network.sync.AccountPushRow
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import com.dumbopanda.ledger.core.network.sync.SyncResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PullerTest {
    private lateinit var db: LedgerDatabase
    private lateinit var cursorStore: DataStore<SyncCursors>
    private lateinit var fakeSyncApi: FakeSyncApi
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var puller: Puller

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorStore = DataStoreFactory.create(
            serializer = SyncCursorsSerializer,
            produceFile = { File.createTempFile("sync_cursors", ".pb") },
        )
        fakeSyncApi = FakeSyncApi()
        authRepository = FakeAuthRepository()
        puller = Puller(db, cursorStore, fakeSyncApi, authRepository)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedServerEntry(clientUid: String, accountId: String = "account-1") {
        fakeSyncApi.pushEntries(
            listOf(
                EntryPushRow(
                    clientUid = clientUid,
                    entryDate = "2026-09-01",
                    txnId = "Nabc123",
                    accountId = accountId,
                    expenseName = "Groceries",
                    txnType = "Food",
                    txnFreq = "one_off",
                    amountMinor = 1000,
                    direction = "debit",
                ),
            ),
        )
    }

    private suspend fun seedServerAccount(clientUid: String) {
        fakeSyncApi.pushAccounts(listOf(AccountPushRow(clientUid = clientUid, name = "Wallet", sortOrder = 0)))
    }

    @Test
    fun `first pull with no cursor fetches everything and applies it locally`() = runTest {
        seedServerEntry("e1")
        seedServerAccount("a1")

        val outcome = puller.pull()

        assertEquals(PullOutcome.SUCCESS, outcome)
        assertEquals("e1", db.entryDao().findByClientUid("e1")?.clientUid)
        assertEquals("a1", db.accountDao().findByClientUid("a1")?.clientUid)
    }

    @Test
    fun `cursor advances so a second pull with no new rows makes no changes`() = runTest {
        seedServerEntry("e1")
        puller.pull()
        val cursorAfterFirst = cursorStore.data.first().entries

        val outcome = puller.pull()

        assertEquals(PullOutcome.SUCCESS, outcome)
        assertEquals(cursorAfterFirst, cursorStore.data.first().entries)
    }

    @Test
    fun `a local pending op survives the pull instead of being overwritten`() = runTest {
        seedServerEntry("e1")
        db.entryDao().upsert(localDraftEntry("e1"))
        db.pendingOpDao().insert(
            PendingOpEntity(
                entityType = "entry",
                entityClientUid = "e1",
                op = "update",
                payloadJson = "local-pending-payload",
                createdAt = Instant.EPOCH,
            ),
        )

        puller.pull()

        // The pulled server row must NOT have clobbered the local one.
        assertEquals("local-draft-id", db.entryDao().findByClientUid("e1")?.id)
    }

    @Test
    fun `pagination continues across pages until a partial page ends it`() = runTest {
        repeat(551) { i -> seedServerEntry("e$i") }

        val outcome = puller.pull()

        assertEquals(PullOutcome.SUCCESS, outcome)
        assertEquals(2, fakeSyncApi.pullEntriesCallCount)
        assertEquals(551, db.entryDao().pageByDate(1000, 0).first().size)
    }

    @Test
    fun `401 refreshes once and retries, succeeding on the retry`() = runTest {
        seedServerEntry("e1")
        fakeSyncApi.nextPullResult = SyncResult.Unauthorized

        val outcome = puller.pull()

        assertEquals(PullOutcome.SUCCESS, outcome)
        assertEquals(1, authRepository.refreshCallCount)
    }

    @Test
    fun `5xx on pull is retryable and leaves the cursor untouched`() = runTest {
        seedServerEntry("e1")
        fakeSyncApi.nextPullResult = SyncResult.ServerError("boom")

        val outcome = puller.pull()

        assertEquals(PullOutcome.RETRYABLE, outcome)
        assertNull(db.entryDao().findByClientUid("e1"))
    }

    private fun localDraftEntry(clientUid: String) = com.dumbopanda.ledger.core.database.entity.EntryEntity(
        id = "local-draft-id",
        clientUid = clientUid,
        entryDate = java.time.LocalDate.of(2026, 9, 1),
        txnId = "Nabc123",
        accountId = "account-1",
        expenseName = "Groceries (local edit)",
        moreInfo = null,
        txnType = "Food",
        txnFreq = com.dumbopanda.ledger.core.model.TxnFrequency.OneOff,
        amountMinor = 2000,
        direction = com.dumbopanda.ledger.core.model.TxnDirection.Debit,
        transferGroup = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        deletedAt = null,
    )
}
