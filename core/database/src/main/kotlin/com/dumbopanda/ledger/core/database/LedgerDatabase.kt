package com.dumbopanda.ledger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dumbopanda.ledger.core.database.converter.EnumConverters
import com.dumbopanda.ledger.core.database.converter.InstantConverter
import com.dumbopanda.ledger.core.database.converter.LocalDateConverter
import com.dumbopanda.ledger.core.database.dao.AccountDao
import com.dumbopanda.ledger.core.database.dao.DeadLetterDao
import com.dumbopanda.ledger.core.database.dao.EntryDao
import com.dumbopanda.ledger.core.database.dao.PendingOpDao
import com.dumbopanda.ledger.core.database.entity.DeadLetterEntity
import com.dumbopanda.ledger.core.database.entity.PendingOpEntity

// Schema v2: adds pending_ops + dead_letter (§5.1, §5.3) at B1, once the op
// log actually exists.
@Database(
    entities = [
        com.dumbopanda.ledger.core.database.entity.EntryEntity::class,
        com.dumbopanda.ledger.core.database.entity.AccountEntity::class,
        PendingOpEntity::class,
        DeadLetterEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class, LocalDateConverter::class, EnumConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun accountDao(): AccountDao
    abstract fun pendingOpDao(): PendingOpDao
    abstract fun deadLetterDao(): DeadLetterDao
}
