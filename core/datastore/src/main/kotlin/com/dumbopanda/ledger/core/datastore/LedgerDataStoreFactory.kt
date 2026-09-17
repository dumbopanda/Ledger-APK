package com.dumbopanda.ledger.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Per-user, matching LedgerDatabaseFactory (§6.4) — a sync cursor or sync-mode
// preference from a previous account has no meaning for the one signed in now.
@Singleton
class LedgerDataStoreFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun syncCursors(userId: String): DataStore<SyncCursors> =
        DataStoreFactory.create(
            serializer = SyncCursorsSerializer,
            produceFile = { File(context.filesDir, "datastore/sync_cursors-$userId.pb") },
        )

    fun userPrefs(userId: String): DataStore<UserPrefs> =
        DataStoreFactory.create(
            serializer = UserPrefsSerializer,
            produceFile = { File(context.filesDir, "datastore/user_prefs-$userId.pb") },
        )
}
