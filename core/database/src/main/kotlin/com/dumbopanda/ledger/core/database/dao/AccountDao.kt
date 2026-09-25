package com.dumbopanda.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query(
        """SELECT * FROM accounts WHERE deletedAt IS NULL
           ORDER BY sortOrder, name COLLATE NOCASE""",
    )
    fun observeAll(): Flow<List<AccountEntity>>

    @Query(
        """SELECT * FROM accounts WHERE deletedAt IS NULL AND archivedAt IS NULL
           ORDER BY sortOrder, name COLLATE NOCASE""",
    )
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("""SELECT * FROM accounts WHERE id = :id""")
    suspend fun findById(id: String): AccountEntity?

    @Query("""SELECT * FROM accounts WHERE clientUid = :clientUid""")
    suspend fun findByClientUid(clientUid: String): AccountEntity?

    // Same reasoning as EntryDao.reconcileServerId.
    @Query("""UPDATE accounts SET id = :serverId, updatedAt = :updatedAt WHERE clientUid = :clientUid""")
    suspend fun reconcileServerId(clientUid: String, serverId: String, updatedAt: Instant)

    // §7.12/C6 --------------------------------------------------------------------------------

    // First-run seeding's own guard (§2: "Seeded Domestic/International/Cash on first run") --
    // checked and acted on inside ONE transaction (AccountRepository.ensureSeeded), so two
    // near-simultaneous callers (e.g. the Add and Accounts tabs both loading at once) can't
    // both see zero and both seed.
    @Query("""SELECT COUNT(*) FROM accounts WHERE deletedAt IS NULL""")
    suspend fun countLive(): Int

    // §7.12: "rename ... case-insensitively unique among live accounts." COLLATE NOCASE
    // matches observeAll()'s own ordering collation rather than introducing a second
    // case-folding convention.
    @Query("""SELECT COUNT(*) FROM accounts WHERE deletedAt IS NULL AND id != :excludingId AND name = :name COLLATE NOCASE""")
    suspend fun countByNameExcluding(name: String, excludingId: String): Int

    @Query("""UPDATE accounts SET name = :name, updatedAt = :updatedAt WHERE id = :id""")
    suspend fun rename(id: String, name: String, updatedAt: Instant)

    // archivedAt = null unarchives -- updatedAt is separate so unarchiving never has to write
    // a null into a NOT NULL column.
    @Query("""UPDATE accounts SET archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :id""")
    suspend fun setArchived(id: String, archivedAt: Instant?, updatedAt: Instant)

    @Query("""UPDATE accounts SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id""")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Instant)

    // §7.14's export: every LIVE account (archived included -- an archived account is still
    // part of "the ledger"), one-shot.
    @Query("""SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE""")
    suspend fun allLive(): List<AccountEntity>
}
