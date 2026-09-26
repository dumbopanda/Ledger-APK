package com.dumbopanda.ledger.core.database

import android.content.Context
import androidx.room.Room
import com.dumbopanda.ledger.core.database.migration.MIGRATION_1_2
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// One file per user (§6.4): "Two accounts on one phone cannot see each other's
// data because they are not in the same file." The caller (core:data, once a
// session exists) is responsible for closing the previous instance on sign-out
// / account switch before opening the next user's file.
@Singleton
class LedgerDatabaseFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun open(userId: String): LedgerDatabase =
        Room.databaseBuilder(context, LedgerDatabase::class.java, fileNameFor(userId))
            .addMigrations(MIGRATION_1_2)
            // Sync and boot open their own short-lived instance of this same file while the UI
            // observes SessionDatabaseHolder's long-lived one. Room only notifies observers of
            // writes made through their OWN instance unless this is on -- without it a send
            // (ops deleted) or a pull (entries inserted) never reached the screen: the sync bar
            // kept saying "1 entry saved on this device" after it had gone.
            .enableMultiInstanceInvalidation()
            .build()

    companion object {
        fun fileNameFor(userId: String): String {
            require(userId.isNotBlank())
            return "ledger-$userId.db"
        }
    }
}
