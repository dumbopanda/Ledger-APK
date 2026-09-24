package com.dumbopanda.ledger.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

// C1's own check, literally: "Offline entry appears instantly, one pending op." Both halves
// of that -- the entity row and its op -- must land together, since OpLog's contract is that
// neither can exist locally without the other.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EntryRepositoryTest {
    private lateinit var db: LedgerDatabase
    private val clock = LedgerClock { Instant.parse("2026-09-24T12:00:00Z") }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun entry(
        id: String = "local-1",
        clientUid: String = "client-uid-1",
        expenseName: String = "Groceries",
        amountMinor: Long = 45_000,
        direction: TxnDirection = TxnDirection.Debit,
        transferGroup: String? = null,
        txnId: TxnId = TxnId("Nabc123"),
    ) = Entry(
        id = id,
        clientUid = clientUid,
        entryDate = LocalDate.of(2026, 9, 24),
        txnId = txnId,
        accountId = "account-1",
        expenseName = expenseName,
        moreInfo = null,
        txnType = "Payment",
        txnFreq = TxnFrequency.OneOff,
        amount = Money(amountMinor),
        direction = direction,
        transferGroup = transferGroup,
        createdAt = Instant.parse("2026-09-24T12:00:00Z"),
    )

    @Test
    fun `addEntry writes the row and enqueues exactly one pending op`() = runTest {
        writeEntry(db, clock, entry())

        val rows = db.entryDao().findByClientUid("client-uid-1")
        assertTrue(rows != null)
        assertEquals("Groceries", rows!!.expenseName)

        val pending = db.pendingOpDao().page(10)
        assertEquals(1, pending.size)
        assertEquals("insert", pending.single().op)
        assertEquals("client-uid-1", pending.single().entityClientUid)
    }

    @Test
    fun `the pending op's payload round-trips the entry's real values, not placeholders`() = runTest {
        writeEntry(db, clock, entry(expenseName = "Rapido", amountMinor = 12_345))

        val payload = db.pendingOpDao().page(10).single().payloadJson
        assertTrue(payload.contains("\"expense_name\":\"Rapido\""))
        assertTrue(payload.contains("\"amount_minor\":12345"))
    }

    @Test
    fun `addTransfer writes both legs and enqueues two pending ops sharing no accidental state`() = runTest {
        val sent = entry(
            id = "local-1", clientUid = "client-sent", direction = TxnDirection.Debit,
            transferGroup = "group-1", txnId = TxnId("Ysent01"),
        )
        val received = entry(
            id = "local-2", clientUid = "client-received", direction = TxnDirection.Credit,
            transferGroup = "group-1", txnId = TxnId("Ysent01"),
        )

        writeTransfer(db, clock, sent, received)

        val legs = db.entryDao().transferLegs("group-1")
        assertEquals(2, legs.size)
        assertEquals(setOf(TxnDirection.Debit, TxnDirection.Credit), legs.map { it.direction }.toSet())

        val pending = db.pendingOpDao().page(10)
        assertEquals(2, pending.size)
        assertEquals(setOf("client-sent", "client-received"), pending.map { it.entityClientUid }.toSet())
    }

    @Test
    fun `addTransfer refuses legs with mismatched transfer groups`() {
        val sent = entry(clientUid = "client-sent", transferGroup = "group-1")
        val received = entry(clientUid = "client-received", transferGroup = "group-2")

        assertThrows(IllegalArgumentException::class.java) {
            runTest { writeTransfer(db, clock, sent, received) }
        }
    }

    @Test
    fun `addTransfer refuses legs with no transfer group at all`() {
        val sent = entry(clientUid = "client-sent", transferGroup = null)
        val received = entry(clientUid = "client-received", transferGroup = null)

        assertThrows(IllegalArgumentException::class.java) {
            runTest { writeTransfer(db, clock, sent, received) }
        }
    }
}
