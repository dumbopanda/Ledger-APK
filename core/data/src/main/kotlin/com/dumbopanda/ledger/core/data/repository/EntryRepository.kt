package com.dumbopanda.ledger.core.data.repository

import androidx.room.withTransaction
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.mapper.toEntity
import com.dumbopanda.ledger.core.data.mapper.toPushRow
import com.dumbopanda.ledger.core.data.sync.Op
import com.dumbopanda.ledger.core.data.sync.OpLog
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Entry
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val ENTRY = "entry"

/**
 * §C1: `addEntry`/`addTransfer`. Both write the entity AND enqueue its op inside one Room
 * transaction, per [OpLog]'s own contract -- there must be no window where a local row exists
 * without a pending op, since that op is the only thing that will ever push it to the server.
 * A transfer's two legs share that same transaction, so a crash between them can't leave a
 * half-written transfer (one leg with no sibling) sitting in the local database.
 */
@Singleton
class EntryRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val clock: LedgerClock,
) {
    suspend fun addEntry(entry: Entry) = writeEntry(sessionDatabaseHolder.database(), clock, entry)

    suspend fun addTransfer(sent: Entry, received: Entry) =
        writeTransfer(sessionDatabaseHolder.database(), clock, sent, received)
}

// Top-level, not a method on EntryRepository, so tests can drive the actual transactional
// logic against a real in-memory Room database directly -- without needing a real signed-in
// session in SessionStore's EncryptedSharedPreferences just to prove a transaction is atomic.
internal suspend fun writeEntry(db: LedgerDatabase, clock: LedgerClock, entry: Entry) {
    db.withTransaction {
        enqueueInsert(db, clock, entry)
    }
}

internal suspend fun writeTransfer(db: LedgerDatabase, clock: LedgerClock, sent: Entry, received: Entry) {
    require(sent.transferGroup != null && sent.transferGroup == received.transferGroup) {
        "addTransfer requires both legs to share a non-null transferGroup"
    }
    db.withTransaction {
        enqueueInsert(db, clock, sent)
        enqueueInsert(db, clock, received)
    }
}

private suspend fun enqueueInsert(db: LedgerDatabase, clock: LedgerClock, entry: Entry) {
    val entity = entry.toEntity()
    db.entryDao().upsert(entity)
    OpLog(db.pendingOpDao(), clock).enqueue(
        Op.Insert(ENTRY, entity.clientUid, Json.encodeToString(entity.toPushRow())),
    )
}
