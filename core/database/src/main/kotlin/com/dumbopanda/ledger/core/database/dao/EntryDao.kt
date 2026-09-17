package com.dumbopanda.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EntryDao {
    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Upsert
    suspend fun upsertAll(vararg entries: EntryEntity)

    @Query(
        """SELECT * FROM entries WHERE deletedAt IS NULL
           ORDER BY entryDate DESC, createdAt DESC LIMIT :limit OFFSET :offset""",
    )
    fun pageByDate(limit: Int, offset: Int): Flow<List<EntryEntity>>

    // The prototype's odd-count-live rule (§2), moved off the main thread and made reactive.
    @Query(
        """SELECT txnId FROM entries WHERE deletedAt IS NULL AND txnId LIKE 'Y%'
           GROUP BY txnId HAVING COUNT(*) % 2 = 1""",
    )
    fun openYIds(): Flow<List<String>>

    @Query("""SELECT * FROM entries WHERE transferGroup = :group AND deletedAt IS NULL""")
    suspend fun transferLegs(group: String): List<EntryEntity>

    // Transfers excluded — "moving your own money is not spending" (§7.7's month
    // summary card explicitly excludes them from Out/In/Net).
    @Query(
        """SELECT SUM(CASE WHEN direction='credit' THEN amountMinor ELSE -amountMinor END)
           FROM entries WHERE deletedAt IS NULL AND transferGroup IS NULL
           AND entryDate BETWEEN :from AND :to""",
    )
    fun netMinorBetween(from: LocalDate, to: LocalDate): Flow<Long?>

    @Query("""SELECT * FROM entries WHERE id = :id""")
    suspend fun findById(id: String): EntryEntity?

    @Query("""SELECT * FROM entries WHERE clientUid = :clientUid""")
    suspend fun findByClientUid(clientUid: String): EntryEntity?
}
