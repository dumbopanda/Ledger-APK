package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import com.dumbopanda.ledger.core.database.LedgerDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * §7.13's Sync section (queue summary, "Send everything now") and dead-letter list. Reactive
 * reads only -- the actual push/pull happens in [SyncScheduler]/`SyncWorker`/`SyncRepository`,
 * none of which this duplicates.
 */
@Singleton
class SyncStatusRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val syncScheduler: SyncScheduler,
) {
    fun pendingOpCount(): Flow<Int> = pendingOpCount { sessionDatabaseHolder.database() }

    // §5.3 rule 4: a 4xx-rejected op, moved here and never retried.
    fun deadLetterCount(): Flow<Int> = deadLetterCount { sessionDatabaseHolder.database() }

    // §5.5 trigger 5 -- "Send everything now," ignoring the settle timer.
    fun sendNow() = syncScheduler.onManualSyncRequested()

    fun oldestPendingAt(): Flow<Instant?> = sessionDatabaseHolder.database().pendingOpDao().observeOldestCreatedAt()

    fun liveEntryCount(): Flow<Int> = sessionDatabaseHolder.database().entryDao().observeLiveCount()

    // History's per-row tags, keyed by the entry's clientUid: a queued insert means the row
    // has never left this device ("on this device"); only a queued update means the server
    // has an older version ("edited"). Delete-only ops don't tag anything -- the row is gone.
    fun entrySyncMarks(): Flow<Map<String, EntrySyncMark>> =
        sessionDatabaseHolder.database().pendingOpDao().observeEntryOps().map { ops ->
            ops.groupBy { it.clientUid }.mapNotNull { (clientUid, rows) ->
                val kinds = rows.map { it.op }.toSet()
                when {
                    "insert" in kinds -> clientUid to EntrySyncMark.OnDevice
                    "update" in kinds -> clientUid to EntrySyncMark.Edited
                    else -> null
                }
            }.toMap()
        }
}

enum class EntrySyncMark { OnDevice, Edited }

internal fun pendingOpCount(databaseProvider: () -> LedgerDatabase): Flow<Int> =
    databaseProvider().pendingOpDao().observeCount()

internal fun deadLetterCount(databaseProvider: () -> LedgerDatabase): Flow<Int> =
    databaseProvider().deadLetterDao().observeAll().map { it.size }
