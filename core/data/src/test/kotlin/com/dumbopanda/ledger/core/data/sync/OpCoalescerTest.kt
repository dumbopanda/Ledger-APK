package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

// §5.2's coalescing table, one test per row.
class OpCoalescerTest {
    private fun pendingOp(op: String, payload: String = "old-payload", seq: Long = 1L) =
        PendingOpEntity(
            seq = seq,
            entityType = "entry",
            entityClientUid = "client-1",
            op = op,
            payloadJson = payload,
            createdAt = Instant.EPOCH,
        )

    private fun insert(payload: String = "payload") = Op.Insert("entry", "client-1", payload)
    private fun update(payload: String = "payload") = Op.Update("entry", "client-1", payload)
    private fun delete(payload: String = "payload") = Op.Delete("entry", "client-1", payload)

    @Test
    fun `none + insert = append insert`() {
        val decision = OpCoalescer.decide(existing = null, incoming = insert())

        assertEquals(OpCoalescer.Decision.Append(insert()), decision)
    }

    @Test
    fun `none + update = append update`() {
        val decision = OpCoalescer.decide(existing = null, incoming = update())

        assertEquals(OpCoalescer.Decision.Append(update()), decision)
    }

    @Test
    fun `none + delete = append delete`() {
        val decision = OpCoalescer.decide(existing = null, incoming = delete())

        assertEquals(OpCoalescer.Decision.Append(delete()), decision)
    }

    @Test
    fun `insert + update = rewrite the pending insert's payload, no new op`() {
        val existing = pendingOp(op = "insert", seq = 7L)

        val decision = OpCoalescer.decide(existing, incoming = update("new-payload"))

        assertEquals(OpCoalescer.Decision.RewritePayload(existingSeq = 7L, payloadJson = "new-payload"), decision)
    }

    @Test
    fun `insert + delete = remove the insert, emit nothing`() {
        val existing = pendingOp(op = "insert", seq = 9L)

        val decision = OpCoalescer.decide(existing, incoming = delete())

        assertEquals(OpCoalescer.Decision.Cancel(existingSeq = 9L), decision)
    }

    @Test
    fun `update + update = replace payload, one op not two`() {
        val existing = pendingOp(op = "update", seq = 3L)

        val decision = OpCoalescer.decide(existing, incoming = update("newer-payload"))

        assertEquals(OpCoalescer.Decision.RewritePayload(existingSeq = 3L, payloadJson = "newer-payload"), decision)
    }

    @Test
    fun `update + delete = drop the update, append the delete`() {
        val existing = pendingOp(op = "update", seq = 5L)
        val incomingDelete = delete("tombstone-payload")

        val decision = OpCoalescer.decide(existing, incoming = incomingDelete)

        assertEquals(OpCoalescer.Decision.DropAndAppend(existingSeq = 5L, op = incomingDelete), decision)
    }

    @Test
    fun `delete + anything is unreachable`() {
        val existing = pendingOp(op = "delete", seq = 11L)

        assertThrows(IllegalStateException::class.java) {
            OpCoalescer.decide(existing, incoming = update())
        }
    }
}
