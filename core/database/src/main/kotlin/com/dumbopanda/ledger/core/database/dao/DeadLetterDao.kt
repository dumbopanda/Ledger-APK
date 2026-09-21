package com.dumbopanda.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dumbopanda.ledger.core.database.entity.DeadLetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadLetterDao {
    @Insert
    suspend fun insert(entity: DeadLetterEntity)

    @Query("SELECT * FROM dead_letter ORDER BY failedAt DESC")
    fun observeAll(): Flow<List<DeadLetterEntity>>
}
