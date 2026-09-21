package com.dumbopanda.ledger.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v1 -> v2: the op log (§5.1) and its dead-letter table (§5.3 rule 4). Neither
// table existed until B1 -- no data to carry over, just create them.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_ops` (
                `seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entityType` TEXT NOT NULL,
                `entityClientUid` TEXT NOT NULL,
                `op` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `lastError` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_ops_entityClientUid` " +
                "ON `pending_ops` (`entityClientUid`)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dead_letter` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entityType` TEXT NOT NULL,
                `entityClientUid` TEXT NOT NULL,
                `op` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `error` TEXT NOT NULL,
                `failedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
