package com.dumbopanda.ledger.core.data.sync

import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import com.dumbopanda.ledger.core.datastore.SyncCursorsSerializer
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import com.dumbopanda.ledger.core.network.sync.SyncResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

// §5's property test. 1,000 random local op sequences, each pushed and
// pulled through a simulated backend (with injected transient failures),
// must converge: no duplicate rows, no lost tombstones, no leftover queue.
// A failure here means the sync engine is wrong, not the test -- per the
// plan, this is not a block to move past by relaxing an assertion.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncPropertyTest {
    private enum class LocalState { NONE, ACTIVE, GONE }

    @Test
    fun `1000 random op sequences converge to a consistent local-server state`() = runTest {
        repeat(1000) { iteration ->
            try {
                runIteration(seed = iteration.toLong())
            } catch (e: AssertionError) {
                fail("Iteration seed=$iteration failed: ${e.message}")
            }
        }
    }

    private suspend fun runIteration(seed: Long) {
        val random = Random(seed)
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val fakeSyncApi = FakeSyncApi()
            val authRepository = FakeAuthRepository()
            val opLog = OpLog(db.pendingOpDao(), LedgerClock { Instant.EPOCH })
            val pusher = Pusher(
                db.pendingOpDao(), db.deadLetterDao(), db.entryDao(), db.accountDao(),
                fakeSyncApi, authRepository, LedgerClock { Instant.EPOCH },
            )
            val cursorStore = DataStoreFactory.create(
                serializer = SyncCursorsSerializer,
                produceFile = { File.createTempFile("sync_cursors_prop", ".pb") },
            )
            val puller = Puller(db, cursorStore, fakeSyncApi, authRepository)

            val clientUids = (0 until 4).map { "e-$seed-$it" }
            val state = mutableMapOf<String, LocalState>()
            var failureBudget = random.nextInt(0, 4)

            repeat(random.nextInt(1, 16)) {
                val clientUid = clientUids.random(random)
                when (state.getOrDefault(clientUid, LocalState.NONE)) {
                    LocalState.NONE -> {
                        applyInsert(db, opLog, clientUid, random)
                        state[clientUid] = LocalState.ACTIVE
                    }

                    LocalState.ACTIVE -> {
                        if (random.nextBoolean()) {
                            applyUpdate(db, opLog, clientUid, random)
                        } else {
                            applyDelete(db, opLog, clientUid)
                            state[clientUid] = LocalState.GONE
                        }
                    }

                    LocalState.GONE -> Unit // unreachable from the UI; nothing to do
                }

                if (random.nextBoolean()) {
                    if (failureBudget > 0 && random.nextBoolean()) {
                        failureBudget--
                        fakeSyncApi.nextPushResult = SyncResult.ServerError("injected transient failure")
                    }
                    drainPush(pusher)
                }
            }

            drainPush(pusher)
            puller.pull()

            assertTrue(
                "seed=$seed: pending ops did not fully drain",
                db.pendingOpDao().page(100).isEmpty(),
            )

            for (clientUid in clientUids) {
                val local = db.entryDao().findByClientUid(clientUid)
                when (state.getOrDefault(clientUid, LocalState.NONE)) {
                    LocalState.NONE -> {
                        assertNull("seed=$seed $clientUid: never touched but has a local row", local)
                        assertNull("seed=$seed $clientUid: never touched but reached the server", fakeSyncApi.entryIsDeleted(clientUid))
                    }

                    LocalState.ACTIVE -> {
                        assertTrue("seed=$seed $clientUid: active but missing locally", local != null && local.deletedAt == null)
                        assertEquals("seed=$seed $clientUid: active but not confirmed live on the server", false, fakeSyncApi.entryIsDeleted(clientUid))
                    }

                    LocalState.GONE -> {
                        val serverDeleted = fakeSyncApi.entryIsDeleted(clientUid)
                        if (serverDeleted == null) {
                            // Cancelled before ever reaching the server (insert+delete): hard-deleted locally too.
                            assertNull("seed=$seed $clientUid: gone-and-never-pushed but a local row survives", local)
                        } else {
                            assertTrue("seed=$seed $clientUid: gone but the server copy isn't deleted", serverDeleted)
                            assertTrue("seed=$seed $clientUid: gone but the local row isn't soft-deleted", local == null || local.deletedAt != null)
                        }
                    }
                }
            }
        } finally {
            db.close()
        }
    }

    private suspend fun drainPush(pusher: Pusher, maxAttempts: Int = 10) {
        repeat(maxAttempts) {
            when (pusher.push()) {
                PushOutcome.SUCCESS -> return
                PushOutcome.RETRYABLE -> Unit // simulates WorkManager's backoff-and-retry
                PushOutcome.SESSION_EXPIRED -> fail("push() reported SessionExpired; no 401s are injected in this test")
            }
        }
    }

    private suspend fun applyInsert(db: LedgerDatabase, opLog: OpLog, clientUid: String, random: Random) {
        val entity = entryEntity(clientUid, id = UUID.randomUUID().toString(), amountMinor = random.nextLong(1, 100_000))
        db.entryDao().upsert(entity)
        opLog.enqueue(Op.Insert("entry", clientUid, entity.toPushRowJson()))
    }

    private suspend fun applyUpdate(db: LedgerDatabase, opLog: OpLog, clientUid: String, random: Random) {
        val current = db.entryDao().findByClientUid(clientUid) ?: return
        val updated = current.copy(amountMinor = random.nextLong(1, 100_000))
        db.entryDao().upsert(updated)
        opLog.enqueue(Op.Update("entry", clientUid, updated.toPushRowJson()))
    }

    private suspend fun applyDelete(db: LedgerDatabase, opLog: OpLog, clientUid: String) {
        val current = db.entryDao().findByClientUid(clientUid) ?: return
        val deletedAt = Instant.EPOCH.plusSeconds(1)
        val outcome = opLog.enqueue(Op.Delete("entry", clientUid, current.toPushRowJson(deletedAt = deletedAt)))
        if (outcome == EnqueueOutcome.CANCELLED) {
            db.entryDao().deleteByClientUid(clientUid)
        } else {
            db.entryDao().upsert(current.copy(deletedAt = deletedAt))
        }
    }

    private fun entryEntity(clientUid: String, id: String, amountMinor: Long) = EntryEntity(
        id = id,
        clientUid = clientUid,
        entryDate = java.time.LocalDate.of(2026, 9, 1),
        txnId = "Nabc123",
        accountId = "account-1",
        expenseName = "Property test entry",
        moreInfo = null,
        txnType = "Misc",
        txnFreq = TxnFrequency.OneOff,
        amountMinor = amountMinor,
        direction = TxnDirection.Debit,
        transferGroup = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        deletedAt = null,
    )

    private fun EntryEntity.toPushRowJson(deletedAt: Instant? = this.deletedAt): String = Json.encodeToString(
        EntryPushRow(
            clientUid = clientUid,
            entryDate = entryDate.toString(),
            txnId = txnId,
            accountId = accountId,
            expenseName = expenseName,
            moreInfo = moreInfo,
            txnType = txnType,
            txnFreq = if (txnFreq == TxnFrequency.OneOff) "one_off" else "monthly",
            amountMinor = amountMinor,
            direction = if (direction == TxnDirection.Debit) "debit" else "credit",
            transferGroup = transferGroup,
            deletedAt = deletedAt?.toString(),
        ),
    )
}
