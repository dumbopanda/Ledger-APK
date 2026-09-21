package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.database.dao.AccountDao
import com.dumbopanda.ledger.core.database.dao.DeadLetterDao
import com.dumbopanda.ledger.core.database.dao.EntryDao
import com.dumbopanda.ledger.core.database.dao.PendingOpDao
import com.dumbopanda.ledger.core.database.entity.DeadLetterEntity
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import com.dumbopanda.ledger.core.network.sync.AccountPushRow
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import com.dumbopanda.ledger.core.network.sync.PushResultRow
import com.dumbopanda.ledger.core.network.sync.SyncApi
import com.dumbopanda.ledger.core.network.sync.SyncResult
import kotlinx.serialization.json.Json
import java.time.Instant

enum class PushOutcome {
    SUCCESS, // queue drained
    RETRYABLE, // 5xx / IO: stopped, every remaining op left queued
    SESSION_EXPIRED, // 401 survived a refresh-and-retry
}

private const val PAGE_SIZE = 100
private const val ENTRY = "entry"
private const val ACCOUNT = "account"

// §5.3. Reads pending ops in seq order, groups contiguous same-entityType
// runs into one network call, and stops at the first non-recoverable
// failure so a later op can never overtake one it depends on.
//
// Not Hilt-managed -- see OpLog's note; constructed by SyncRepository per
// active session with that user's LedgerDatabase.
class Pusher(
    private val pendingOpDao: PendingOpDao,
    private val deadLetterDao: DeadLetterDao,
    private val entryDao: EntryDao,
    private val accountDao: AccountDao,
    private val syncApi: SyncApi,
    private val authRepository: AuthRepository,
    private val clock: LedgerClock,
) {
    suspend fun push(): PushOutcome {
        var refreshAttempted = false

        while (true) {
            val page = pendingOpDao.page(PAGE_SIZE)
            if (page.isEmpty()) return PushOutcome.SUCCESS

            for (group in page.groupContiguousByEntityType()) {
                val (outcome, updatedFlag) = processGroup(group, refreshAttempted)
                refreshAttempted = updatedFlag
                if (outcome != PushOutcome.SUCCESS) return outcome
            }
        }
    }

    private suspend fun processGroup(
        group: List<PendingOpEntity>,
        refreshAttempted: Boolean,
    ): Pair<PushOutcome, Boolean> {
        val (result, updatedFlag) = sendWithAuthRetry(group, refreshAttempted)

        return when (result) {
            is SyncResult.Success -> {
                applySuccess(group, result.value)
                PushOutcome.SUCCESS to updatedFlag
            }

            is SyncResult.Unauthorized -> PushOutcome.SESSION_EXPIRED to updatedFlag
            is SyncResult.ServerError -> PushOutcome.RETRYABLE to updatedFlag

            is SyncResult.ClientError -> {
                if (group.size == 1) {
                    deadLetter(group.single(), result.message)
                    PushOutcome.SUCCESS to updatedFlag
                } else {
                    // push_entries is one atomic statement -- a single bad
                    // row aborts the whole batch. Retry one op at a time so
                    // it can't block or dead-letter its innocent batch-mates.
                    var flag = updatedFlag
                    for (op in group) {
                        val (subOutcome, subFlag) = processGroup(listOf(op), flag)
                        flag = subFlag
                        if (subOutcome != PushOutcome.SUCCESS) return subOutcome to flag
                    }
                    PushOutcome.SUCCESS to flag
                }
            }
        }
    }

    private suspend fun sendWithAuthRetry(
        group: List<PendingOpEntity>,
        refreshAttempted: Boolean,
    ): Pair<SyncResult<List<PushResultRow>>, Boolean> {
        val first = sendGroup(group)
        if (first !is SyncResult.Unauthorized) return first to refreshAttempted
        if (refreshAttempted) return first to refreshAttempted

        val refreshed = authRepository.refreshSession()
        return if (refreshed) sendGroup(group) to true else first to true
    }

    private suspend fun sendGroup(group: List<PendingOpEntity>): SyncResult<List<PushResultRow>> =
        when (group.first().entityType) {
            ENTRY -> syncApi.pushEntries(group.map { Json.decodeFromString<EntryPushRow>(it.payloadJson) })
            ACCOUNT -> syncApi.pushAccounts(group.map { Json.decodeFromString<AccountPushRow>(it.payloadJson) })
            else -> error("Unknown entityType: ${group.first().entityType}")
        }

    private suspend fun applySuccess(group: List<PendingOpEntity>, results: List<PushResultRow>) {
        val byClientUid = results.associateBy { it.clientUid }
        val dao = if (group.first().entityType == ENTRY) entryDao::reconcileServerId else accountDao::reconcileServerId

        group.forEach { op ->
            val row = byClientUid[op.entityClientUid] ?: return@forEach
            dao(op.entityClientUid, row.id, Instant.parse(row.updatedAt))
        }
        pendingOpDao.deleteBySeqs(group.map { it.seq })
    }

    private suspend fun deadLetter(op: PendingOpEntity, reason: String) {
        deadLetterDao.insert(
            DeadLetterEntity(
                entityType = op.entityType,
                entityClientUid = op.entityClientUid,
                op = op.op,
                payloadJson = op.payloadJson,
                error = reason,
                failedAt = clock.now(),
            ),
        )
        pendingOpDao.deleteBySeq(op.seq)
    }
}

private fun List<PendingOpEntity>.groupContiguousByEntityType(): List<List<PendingOpEntity>> {
    if (isEmpty()) return emptyList()
    val groups = mutableListOf<MutableList<PendingOpEntity>>(mutableListOf(first()))
    for (op in drop(1)) {
        if (op.entityType == groups.last().last().entityType) {
            groups.last().add(op)
        } else {
            groups.add(mutableListOf(op))
        }
    }
    return groups
}
