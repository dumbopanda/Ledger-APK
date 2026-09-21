package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.dao.PendingOpDao
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import java.time.Instant

enum class EnqueueOutcome {
    // A pending op now represents this entity (possibly coalesced into an
    // existing row).
    QUEUED,

    // insert+delete: the entity never reached the server, so nothing is
    // queued. The caller must hard-delete the local row -- there is nothing
    // left to soft-delete a tombstone for.
    CANCELLED,
}

// §5.1/§5.2. The caller is expected to invoke `enqueue` inside the same Room
// transaction as the entity-table write (§6.5) -- there must be no window
// where a row exists locally without a pending op, or the reverse.
//
// Not Hilt-managed: its DAO comes from the signed-in user's LedgerDatabase
// (§6.4, one file per user), which only exists once SyncRepository opens it.
// Constructed there, per active session.
class OpLog(
    private val pendingOpDao: PendingOpDao,
    private val clock: LedgerClock,
) {
    suspend fun enqueue(op: Op): EnqueueOutcome {
        val existing = pendingOpDao.latestFor(op.entityClientUid)
        return when (val decision = OpCoalescer.decide(existing, op)) {
            is OpCoalescer.Decision.Append -> {
                pendingOpDao.insert(decision.op.toEntity(clock.now()))
                EnqueueOutcome.QUEUED
            }

            is OpCoalescer.Decision.RewritePayload -> {
                pendingOpDao.updatePayload(decision.existingSeq, decision.payloadJson)
                EnqueueOutcome.QUEUED
            }

            is OpCoalescer.Decision.Cancel -> {
                pendingOpDao.deleteBySeq(decision.existingSeq)
                EnqueueOutcome.CANCELLED
            }

            is OpCoalescer.Decision.DropAndAppend -> {
                pendingOpDao.deleteBySeq(decision.existingSeq)
                pendingOpDao.insert(decision.op.toEntity(clock.now()))
                EnqueueOutcome.QUEUED
            }
        }
    }
}

private fun Op.toEntity(createdAt: Instant) = PendingOpEntity(
    entityType = entityType,
    entityClientUid = entityClientUid,
    op = typeName(),
    payloadJson = payloadJson,
    createdAt = createdAt,
)
