package com.dumbopanda.ledger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dumbopanda.ledger.core.database.converter.EnumConverters
import com.dumbopanda.ledger.core.database.converter.InstantConverter
import com.dumbopanda.ledger.core.database.converter.LocalDateConverter
import com.dumbopanda.ledger.core.database.dao.AccountDao
import com.dumbopanda.ledger.core.database.dao.EntryDao

// Schema v1: entries + accounts only. pending_ops / dead_letter (§6.4's module
// listing) land in a v2 migration at B1, once the op log actually exists — no
// point exporting a schema for a table nothing writes to yet.
@Database(
    entities = [
        com.dumbopanda.ledger.core.database.entity.EntryEntity::class,
        com.dumbopanda.ledger.core.database.entity.AccountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class, LocalDateConverter::class, EnumConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun accountDao(): AccountDao
}
