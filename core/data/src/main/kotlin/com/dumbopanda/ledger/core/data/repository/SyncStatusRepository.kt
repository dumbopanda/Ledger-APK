package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import com.dumbopanda.ledger.core.database.LedgerDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}

internal fun pendingOpCount(databaseProvider: () -> LedgerDatabase): Flow<Int> =
    databaseProvider().pendingOpDao().observeCount()

internal fun deadLetterCount(databaseProvider: () -> LedgerDatabase): Flow<Int> =
    databaseProvider().deadLetterDao().observeAll().map { it.size }
