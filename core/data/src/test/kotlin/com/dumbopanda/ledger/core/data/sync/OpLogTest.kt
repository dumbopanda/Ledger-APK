package com.dumbopanda.ledger.core.data.sync

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpLogTest {
    private lateinit var db: LedgerDatabase
    private lateinit var opLog: OpLog

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        opLog = OpLog(db.pendingOpDao(), LedgerClock { Instant.EPOCH })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `enqueue with no existing op appends a new row`() = runTest {
        val outcome = opLog.enqueue(Op.Insert("entry", "client-1", "payload"))

        assertEquals(EnqueueOutcome.QUEUED, outcome)
        val stored = db.pendingOpDao().latestFor("client-1")
        assertEquals("insert", stored?.op)
        assertEquals("payload", stored?.payloadJson)
    }

    @Test
    fun `insert then update coalesces into one row and keeps the insert op`() = runTest {
        opLog.enqueue(Op.Insert("entry", "client-1", "first"))
        val outcome = opLog.enqueue(Op.Update("entry", "client-1", "second"))

        assertEquals(EnqueueOutcome.QUEUED, outcome)
        val stored = db.pendingOpDao().latestFor("client-1")
        assertEquals("insert", stored?.op)
        assertEquals("second", stored?.payloadJson)
        assertEquals(1, db.pendingOpDao().page(10).size)
    }

    @Test
    fun `insert then delete cancels, no row left`() = runTest {
        opLog.enqueue(Op.Insert("entry", "client-1", "first"))
        val outcome = opLog.enqueue(Op.Delete("entry", "client-1", "tombstone"))

        assertEquals(EnqueueOutcome.CANCELLED, outcome)
        assertNull(db.pendingOpDao().latestFor("client-1"))
    }

    @Test
    fun `update then delete drops the update and appends the delete at a new seq`() = runTest {
        opLog.enqueue(Op.Update("entry", "client-1", "first"))
        val firstSeq = db.pendingOpDao().latestFor("client-1")!!.seq

        opLog.enqueue(Op.Delete("entry", "client-1", "tombstone"))

        val stored = db.pendingOpDao().latestFor("client-1")
        assertEquals("delete", stored?.op)
        assertEquals("tombstone", stored?.payloadJson)
        assert(stored!!.seq > firstSeq)
        assertEquals(1, db.pendingOpDao().page(10).size)
    }

    @Test
    fun `enqueue after a delete is unreachable`() = runTest {
        opLog.enqueue(Op.Insert("entry", "client-1", "first"))
        opLog.enqueue(Op.Delete("entry", "client-1", "tombstone"))
        // Cancel removed the row entirely (insert+delete), so force a delete
        // op to persist by going through update+delete instead.
        opLog.enqueue(Op.Update("entry", "client-2", "first"))
        opLog.enqueue(Op.Delete("entry", "client-2", "tombstone"))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { opLog.enqueue(Op.Update("entry", "client-2", "anything")) }
        }
    }

    @Test
    fun `transfer pair atomicity - both legs enqueued in one transaction land at adjacent seqs`() = runTest {
        db.withTransaction {
            opLog.enqueue(Op.Insert("entry", "leg-out", "debit-payload"))
            opLog.enqueue(Op.Insert("entry", "leg-in", "credit-payload"))
        }

        val page = db.pendingOpDao().page(10)
        assertEquals(2, page.size)
        assertEquals(listOf("leg-out", "leg-in"), page.map { it.entityClientUid })
        assertEquals(page[0].seq + 1, page[1].seq)
    }
}
