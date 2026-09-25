package com.dumbopanda.ledger.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
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

    // §7.7's History list, as a Paging3 source (C1) -- same order as [pageByDate], Room
    // generates the LIMIT/OFFSET windowing itself from this signature.
    @Query(
        """SELECT * FROM entries WHERE deletedAt IS NULL
           ORDER BY entryDate DESC, createdAt DESC""",
    )
    fun pagingSource(): PagingSource<Int, EntryEntity>

    // The prototype's odd-count-live rule (§2), moved off the main thread and made reactive.
    @Query(
        """SELECT txnId FROM entries WHERE deletedAt IS NULL AND txnId LIKE 'Y%'
           GROUP BY txnId HAVING COUNT(*) % 2 = 1""",
    )
    fun openYIds(): Flow<List<String>>

    // `ui-spec.md`'s Owed to me (History > Owed to me, changed 24-25 Sep 2026): every LIVE
    // leg of every Y-id ever seen, open or closed -- not just the currently-open ones
    // `openYIds()` returns. `GroupOwedToMeUseCase` (core:domain) folds this into one
    // open-or-closed row per id; grouping happens there, not in SQL, same split as
    // `pagingSource()` + `GroupEntriesByDayUseCase`.
    @Query("""SELECT * FROM entries WHERE deletedAt IS NULL AND txnId LIKE 'Y%'""")
    fun liveYEntries(): Flow<List<EntryEntity>>

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

    // §7.7's month summary card's other two tiles -- Out and In separately, same exclusion.
    @Query(
        """SELECT COALESCE(SUM(amountMinor), 0) FROM entries
           WHERE deletedAt IS NULL AND transferGroup IS NULL AND direction = 'debit'
           AND entryDate BETWEEN :from AND :to""",
    )
    fun outMinorBetween(from: LocalDate, to: LocalDate): Flow<Long>

    @Query(
        """SELECT COALESCE(SUM(amountMinor), 0) FROM entries
           WHERE deletedAt IS NULL AND transferGroup IS NULL AND direction = 'credit'
           AND entryDate BETWEEN :from AND :to""",
    )
    fun inMinorBetween(from: LocalDate, to: LocalDate): Flow<Long>

    @Query("""SELECT * FROM entries WHERE id = :id""")
    suspend fun findById(id: String): EntryEntity?

    @Query("""SELECT * FROM entries WHERE clientUid = :clientUid""")
    suspend fun findByClientUid(clientUid: String): EntryEntity?

    // After a successful push (§5.3 step 3): the server assigns its own id
    // (push_entries never accepts a client-supplied one), so the local row
    // -- created with a locally-generated id -- must adopt it. Otherwise the
    // next pull upserts a second, server-id-keyed copy of the same row.
    @Query("""UPDATE entries SET id = :serverId, updatedAt = :updatedAt WHERE clientUid = :clientUid""")
    suspend fun reconcileServerId(clientUid: String, serverId: String, updatedAt: Instant)

    // §5.2's insert+delete rule: the entity never reached the server, so
    // there is nothing to keep a tombstone for -- unlike every other delete
    // path, which soft-deletes (sets deletedAt) instead.
    @Query("""DELETE FROM entries WHERE clientUid = :clientUid""")
    suspend fun deleteByClientUid(clientUid: String)

    // §7.11's ordinary delete path (the entry already exists, possibly already synced):
    // soft-delete, never a hard delete, so a synced row still has a tombstone to push.
    @Query("""UPDATE entries SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id""")
    suspend fun softDelete(id: String, deletedAt: Instant)
}
