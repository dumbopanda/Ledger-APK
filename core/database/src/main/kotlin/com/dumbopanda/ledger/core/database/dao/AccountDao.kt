package com.dumbopanda.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

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
}
