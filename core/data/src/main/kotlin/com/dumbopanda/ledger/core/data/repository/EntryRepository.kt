package com.dumbopanda.ledger.core.data.repository

import androidx.room.withTransaction
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.mapper.toDomain
import com.dumbopanda.ledger.core.data.mapper.toEntity
import com.dumbopanda.ledger.core.data.mapper.toPushRow
import com.dumbopanda.ledger.core.data.sync.EnqueueOutcome
import com.dumbopanda.ledger.core.data.sync.Op
import com.dumbopanda.ledger.core.data.sync.OpLog
import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.OPENING_BALANCE
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
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
 *
 * §7.11's `deleteEntry` (C5): soft-deletes (never hard-deletes a synced row -- the tombstone
 * IS the delete, per §5.2), cascades to every leg sharing a `transferGroup`, and hard-deletes
 * instead only when [OpLog] reports the row was never synced in the first place
 * ([EnqueueOutcome.CANCELLED] -- insert+delete, nothing to keep a tombstone for).
 *
 * §7.6's `updateEntry`/`updateTransfer` (C4): same shared-transaction contract, but the
 * caller passes an [Entry] carrying the ORIGINAL row's `id`/`clientUid`/`createdAt` -- "frozen
 * at the moment editing starts" -- so this always overwrites that exact row (`@Upsert` keys on
 * `id`) and enqueues an [Op.Update], never a second insert.
 *
 * **Found and fixed 25 Sep 2026, C7:** every write here enqueues a pending op, but nothing
 * was ever calling [SyncScheduler] -- §5.5/§5.6's trigger 3 ("an op was just enqueued") was
 * built in Sprint B and never wired to a real caller, so no write anywhere in the app has ever
 * actually triggered a push. Every public method now notifies the scheduler after its
 * transaction commits (never inside `withTransaction` -- WorkManager/DataStore I/O has no
 * business blocking a Room transaction), using whatever [UserPrefsRepository.currentSyncMode]
 * currently says.
 */
@Singleton
class EntryRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val syncScheduler: SyncScheduler,
    private val userPrefsRepository: UserPrefsRepository,
    private val clock: LedgerClock,
) {
    suspend fun addEntry(entry: Entry) {
        writeEntry(sessionDatabaseHolder.database(), clock, entry)
        notifySync()
    }

    suspend fun addTransfer(sent: Entry, received: Entry) {
        writeTransfer(sessionDatabaseHolder.database(), clock, sent, received)
        notifySync()
    }

    suspend fun updateEntry(entry: Entry) {
        writeEntryUpdate(sessionDatabaseHolder.database(), clock, entry)
        notifySync()
    }

    suspend fun updateTransfer(sent: Entry, received: Entry) {
        writeTransferUpdate(sessionDatabaseHolder.database(), clock, sent, received)
        notifySync()
    }

    suspend fun deleteEntry(id: String) {
        val anyQueued = deleteEntry(sessionDatabaseHolder.database(), clock, id)
        if (anyQueued) notifySync()
    }

    // Signed opening balance per account id: money in it (+) or owed on it (-), e.g. a credit card.
    fun openingBalances(): Flow<Map<String, Long>> =
        sessionDatabaseHolder.database().entryDao().observeByType(OPENING_BALANCE).map { rows ->
            rows.map { it.toDomain() }.associate { e ->
                e.accountId to if (e.direction == TxnDirection.Credit) e.amount.minor else -e.amount.minor
            }
        }

    // Dated on the account's earliest real entry (today if it has none), so every month's
    // balance -- not just this one's -- starts from the right number. Zero removes it.
    suspend fun setOpeningBalance(accountId: String, signedMinor: Long) {
        val dao = sessionDatabaseHolder.database().entryDao()
        val existing = dao.findByAccountAndType(accountId, OPENING_BALANCE)?.toDomain()
        if (signedMinor == 0L) {
            if (existing != null) deleteEntry(existing.id)
            return
        }
        val today = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val date = dao.earliestDateFor(accountId, OPENING_BALANCE)?.let { minOf(it, today) } ?: today
        val direction = if (signedMinor > 0) TxnDirection.Credit else TxnDirection.Debit
        val amount = Money(abs(signedMinor))
        if (existing != null) {
            updateEntry(existing.copy(amount = amount, direction = direction, entryDate = minOf(existing.entryDate, date)))
        } else {
            addEntry(
                Entry(
                    id = UUID.randomUUID().toString(),
                    clientUid = UUID.randomUUID().toString(),
                    entryDate = date,
                    txnId = TxnId("N" + (1..6).map { TXN_ALPHABET.random() }.joinToString("")),
                    accountId = accountId,
                    expenseName = OPENING_BALANCE,
                    moreInfo = null,
                    txnType = OPENING_BALANCE,
                    txnFreq = TxnFrequency.Monthly,
                    amount = amount,
                    direction = direction,
                    transferGroup = null,
                    createdAt = clock.now(),
                ),
            )
        }
    }

    // §7.10's From/To account display for a transfer leg -- a one-off lookup, not reactive
    // (unlike owedToMeEntries, the detail sheet doesn't need every transfer group live, only
    // the one currently open).
    suspend fun transferLegs(group: String): List<Entry> =
        sessionDatabaseHolder.database().entryDao().transferLegs(group).map { it.toDomain() }

    private suspend fun notifySync() {
        syncScheduler.onOpEnqueued(userPrefsRepository.currentSyncMode())
    }
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

internal suspend fun writeEntryUpdate(db: LedgerDatabase, clock: LedgerClock, entry: Entry) {
    db.withTransaction {
        enqueueUpdate(db, clock, entry)
    }
}

internal suspend fun writeTransferUpdate(db: LedgerDatabase, clock: LedgerClock, sent: Entry, received: Entry) {
    require(sent.transferGroup != null && sent.transferGroup == received.transferGroup) {
        "updateTransfer requires both legs to share a non-null transferGroup"
    }
    db.withTransaction {
        enqueueUpdate(db, clock, sent)
        enqueueUpdate(db, clock, received)
    }
}

private suspend fun enqueueUpdate(db: LedgerDatabase, clock: LedgerClock, entry: Entry) {
    val entity = entry.toEntity(updatedAt = clock.now())
    db.entryDao().upsert(entity)
    OpLog(db.pendingOpDao(), clock).enqueue(
        Op.Update(ENTRY, entity.clientUid, Json.encodeToString(entity.toPushRow())),
    )
}

// Returns whether ANY leg actually queued a pending op -- false when every leg was
// never-synced (CANCELLED), which is the one case the caller must NOT bother the sync
// scheduler about, since there is nothing left to push.
internal suspend fun deleteEntry(db: LedgerDatabase, clock: LedgerClock, id: String): Boolean {
    var anyQueued = false
    db.withTransaction {
        val entity = db.entryDao().findById(id) ?: return@withTransaction
        val legs = entity.transferGroup?.let { db.entryDao().transferLegs(it) } ?: listOf(entity)
        val deletedAt = clock.now()
        val opLog = OpLog(db.pendingOpDao(), clock)
        for (leg in legs) {
            val payload = Json.encodeToString(leg.copy(deletedAt = deletedAt).toPushRow())
            val outcome = opLog.enqueue(Op.Delete(ENTRY, leg.clientUid, payload))
            when (outcome) {
                // Never synced -- nothing to keep a tombstone for, same as §5.2's
                // insert+delete rule for the entity table, not just the op log.
                EnqueueOutcome.CANCELLED -> db.entryDao().deleteByClientUid(leg.clientUid)
                EnqueueOutcome.QUEUED -> {
                    db.entryDao().softDelete(leg.id, deletedAt)
                    anyQueued = true
                }
            }
        }
    }
    return anyQueued
}

private const val TXN_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
