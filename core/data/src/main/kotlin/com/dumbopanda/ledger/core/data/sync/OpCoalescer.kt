package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.database.entity.PendingOpEntity

// §5.2's coalescing table, as a pure decision function. Applied inside the
// same Room transaction as the entity-table write, at the moment the op is
// enqueued -- this is what makes "create -> edit -> delete offline" produce
// no row on the server rather than three round trips.
object OpCoalescer {
    sealed interface Decision {
        // No pending op existed for this client_uid yet.
        data class Append(val op: Op) : Decision

        // insert+update, update+update: same op type survives, only the
        // payload changes. "No new op" / "one op, not two."
        data class RewritePayload(val existingSeq: Long, val payloadJson: String) : Decision

        // insert+delete: the entity never reached the server. Remove the
        // pending insert and emit nothing over the network.
        data class Cancel(val existingSeq: Long) : Decision

        // update+delete: drop the update, append the delete.
        data class DropAndAppend(val existingSeq: Long, val op: Op) : Decision
    }

    fun decide(existing: PendingOpEntity?, incoming: Op): Decision {
        if (existing == null) return Decision.Append(incoming)

        check(existing.op != DELETE) {
            "Unreachable: a deleted row cannot be reached in the UI (existing seq=${existing.seq})"
        }

        return when (incoming) {
            // A client_uid is assigned once, at creation. It can only collide
            // with an existing pending op if that op is for the same entity,
            // which an Insert -- by construction -- never is.
            is Op.Insert -> error("Unreachable: insert collided with an existing pending op")
            is Op.Update -> Decision.RewritePayload(existing.seq, incoming.payloadJson)
            is Op.Delete ->
                if (existing.op == INSERT) {
                    Decision.Cancel(existing.seq)
                } else {
                    Decision.DropAndAppend(existing.seq, incoming)
                }
        }
    }

    private const val INSERT = "insert"
    private const val DELETE = "delete"
}
