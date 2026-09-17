package com.dumbopanda.ledger.core.database

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, LedgerDatabase::class.java, fileNameFor(userId)).build()

    companion object {
        fun fileNameFor(userId: String): String {
            require(userId.isNotBlank())
            return "ledger-$userId.db"
        }
    }
}
