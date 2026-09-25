package com.dumbopanda.ledger.core.data.sync

import androidx.datastore.core.DataStore
import androidx.room.withTransaction
import com.dumbopanda.ledger.core.data.mapper.toEntity
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.datastore.SyncCursor
import com.dumbopanda.ledger.core.datastore.SyncCursors
import com.dumbopanda.ledger.core.datastore.copy
import com.dumbopanda.ledger.core.datastore.syncCursor
import com.dumbopanda.ledger.core.network.dto.AccountDto
import com.dumbopanda.ledger.core.network.dto.EntryDto
import com.dumbopanda.ledger.core.network.sync.SyncApi
import com.dumbopanda.ledger.core.network.sync.SyncResult
import kotlinx.coroutines.flow.first
import java.time.Instant

enum class PullOutcome { SUCCESS, RETRYABLE, SESSION_EXPIRED }

private const val PAGE_SIZE = 500

// §5.4. One page per round trip; the cursor advances only after that page's
// entity upserts commit, so a crash mid-pull just re-fetches and re-upserts
// the same page (harmless) rather than skipping or duplicating rows.
//
// Not Hilt-managed -- see Pusher's note; constructed by SyncRepository per
// active session.
class Puller(
    private val db: LedgerDatabase,
    private val cursorStore: DataStore<SyncCursors>,
    private val syncApi: SyncApi,
    private val authRepository: AuthRepository,
) {
    // onProgress's own argument is a running total across BOTH entries and accounts pages, in
    // that order -- §7.4's "determinate progress from pull_entries page counts," extended to
    // cover accounts too since Restoring has no way to tell the user apart which table a count
    // came from, and doesn't need to.
    suspend fun pull(onProgress: (Int) -> Unit = {}): PullOutcome {
        var pulledSoFar = 0
        val entriesOutcome = pullEntries { pulledSoFar += it; onProgress(pulledSoFar) }
        if (entriesOutcome != PullOutcome.SUCCESS) return entriesOutcome
        return pullAccounts { pulledSoFar += it; onProgress(pulledSoFar) }
    }

    private suspend fun pullEntries(onPage: (Int) -> Unit): PullOutcome {
        var refreshAttempted = false
        while (true) {
            val cursor = cursorStore.data.first().entries
            val (result, updatedFlag) = fetchWithAuthRetry(refreshAttempted) {
                syncApi.pullEntries(cursor.sinceMillisOrNull(), cursor.sinceIdOrNull(), PAGE_SIZE)
            }
            refreshAttempted = updatedFlag

            when (result) {
                is SyncResult.Unauthorized -> return PullOutcome.SESSION_EXPIRED
                is SyncResult.ServerError -> return PullOutcome.RETRYABLE
                is SyncResult.ClientError -> return PullOutcome.RETRYABLE
                is SyncResult.Success -> {
                    val rows = result.value
                    if (rows.isNotEmpty()) applyEntryPage(rows)
                    onPage(rows.size)
                    if (rows.size < PAGE_SIZE) return PullOutcome.SUCCESS
                }
            }
        }
    }

    private suspend fun pullAccounts(onPage: (Int) -> Unit): PullOutcome {
        var refreshAttempted = false
        while (true) {
            val cursor = cursorStore.data.first().accounts
            val (result, updatedFlag) = fetchWithAuthRetry(refreshAttempted) {
                syncApi.pullAccounts(cursor.sinceMillisOrNull(), cursor.sinceIdOrNull(), PAGE_SIZE)
            }
            refreshAttempted = updatedFlag

            when (result) {
                is SyncResult.Unauthorized -> return PullOutcome.SESSION_EXPIRED
                is SyncResult.ServerError -> return PullOutcome.RETRYABLE
                is SyncResult.ClientError -> return PullOutcome.RETRYABLE
                is SyncResult.Success -> {
                    val rows = result.value
                    if (rows.isNotEmpty()) applyAccountPage(rows)
                    onPage(rows.size)
                    if (rows.size < PAGE_SIZE) return PullOutcome.SUCCESS
                }
            }
        }
    }

    private suspend fun applyEntryPage(rows: List<EntryDto>) {
        val pendingOpDao = db.pendingOpDao()
        val entryDao = db.entryDao()
        db.withTransaction {
            for (row in rows) {
                // Conflict rule (§5.4): a pending op is a user intention not
                // yet sent, so it survives the pull -- the row is skipped
                // here and pushed afterwards instead of being overwritten.
                if (pendingOpDao.latestFor(row.clientUid) == null) {
                    entryDao.upsert(row.toEntity())
                }
            }
        }
        advanceEntriesCursor(rows.last())
    }

    private suspend fun applyAccountPage(rows: List<AccountDto>) {
        val pendingOpDao = db.pendingOpDao()
        val accountDao = db.accountDao()
        db.withTransaction {
            for (row in rows) {
                if (pendingOpDao.latestFor(row.clientUid) == null) {
                    accountDao.upsert(row.toEntity())
                }
            }
        }
        advanceAccountsCursor(rows.last())
    }

    private suspend fun advanceEntriesCursor(last: EntryDto) {
        cursorStore.updateData { cursors ->
            cursors.copy { entries = cursorFor(last.updatedAt, last.id) }
        }
    }

    private suspend fun advanceAccountsCursor(last: AccountDto) {
        cursorStore.updateData { cursors ->
            cursors.copy { accounts = cursorFor(last.updatedAt, last.id) }
        }
    }

    private fun cursorFor(updatedAt: String, id: String) = syncCursor {
        lastUpdatedAtEpochMillis = Instant.parse(updatedAt).toEpochMilli()
        lastId = id
        hasPulled = true
    }

    private suspend fun <T> fetchWithAuthRetry(
        refreshAttempted: Boolean,
        request: suspend () -> SyncResult<T>,
    ): Pair<SyncResult<T>, Boolean> {
        val first = request()
        if (first !is SyncResult.Unauthorized) return first to refreshAttempted
        if (refreshAttempted) return first to refreshAttempted

        val refreshed = authRepository.refreshSession()
        return if (refreshed) request() to true else first to true
    }
}

private fun SyncCursor.sinceMillisOrNull(): Long? = if (hasPulled) lastUpdatedAtEpochMillis else null
private fun SyncCursor.sinceIdOrNull(): String? = if (hasPulled) lastId else null
