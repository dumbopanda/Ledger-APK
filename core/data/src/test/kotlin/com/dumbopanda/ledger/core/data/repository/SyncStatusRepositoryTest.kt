package com.dumbopanda.ledger.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.entity.DeadLetterEntity
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
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
class SyncStatusRepositoryTest {
    private lateinit var db: LedgerDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `pendingOpCount reflects the pending_ops table`() = runTest {
        assertEquals(0, pendingOpCount { db }.first())
        db.pendingOpDao().insert(
            PendingOpEntity(entityType = "entry", entityClientUid = "c1", op = "insert", payloadJson = "{}", createdAt = Instant.now()),
        )
        assertEquals(1, pendingOpCount { db }.first())
    }

    @Test
    fun `deadLetterCount reflects the dead_letter table`() = runTest {
        assertEquals(0, deadLetterCount { db }.first())
        db.deadLetterDao().insert(
            DeadLetterEntity(entityType = "entry", entityClientUid = "c1", op = "insert", payloadJson = "{}", error = "400", failedAt = Instant.now()),
        )
        assertEquals(1, deadLetterCount { db }.first())
    }
}
