package com.dumbopanda.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpDao {
    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops WHERE entityClientUid = :clientUid ORDER BY seq DESC LIMIT 1")
    suspend fun latestFor(clientUid: String): PendingOpEntity?

    @Query("UPDATE pending_ops SET payloadJson = :payloadJson WHERE seq = :seq")
    suspend fun updatePayload(seq: Long, payloadJson: String)

    @Query("UPDATE pending_ops SET attempts = attempts + 1, lastError = :error WHERE seq IN (:seqs)")
    suspend fun recordAttemptFailure(seqs: List<Long>, error: String)

    @Query("DELETE FROM pending_ops WHERE seq = :seq")
    suspend fun deleteBySeq(seq: Long)

    @Query("DELETE FROM pending_ops WHERE seq IN (:seqs)")
    suspend fun deleteBySeqs(seqs: List<Long>)

    // Pushed in seq order (§5.1) so a later op can never overtake one it depends on.
    @Query("SELECT * FROM pending_ops ORDER BY seq ASC LIMIT :limit")
    suspend fun page(limit: Int): List<PendingOpEntity>

    @Query("SELECT COUNT(*) FROM pending_ops")
    fun observeCount(): Flow<Int>

    @Query("SELECT entityClientUid AS clientUid, op FROM pending_ops WHERE entityType = 'entry'")
    fun observeEntryOps(): Flow<List<PendingEntryOpRow>>

    // When the oldest waiting op was queued -- Batched mode never holds anything past an hour
    // from this, which is the "by 02:15" in Settings' "Waiting to be sent" line.
    @Query("SELECT MIN(createdAt) FROM pending_ops")
    fun observeOldestCreatedAt(): Flow<java.time.Instant?>
}
