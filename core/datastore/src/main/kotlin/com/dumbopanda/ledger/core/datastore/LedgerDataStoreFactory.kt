package com.dumbopanda.ledger.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

// Per-user, matching LedgerDatabaseFactory (§6.4) — a sync cursor or sync-mode
// preference from a previous account has no meaning for the one signed in now.
//
// DataStore throws IllegalStateException if two live instances ever point at the same file
// (26 Sep 2026, real crash right after sign-in: a long-lived UI collector on userPrefs/
// consentState overlapped a one-shot read/write elsewhere, each having created its own fresh
// instance). Every store below is therefore memoized here -- exactly one DataStore per file for
// the process's lifetime -- rather than created fresh on each call.
@Singleton
class LedgerDataStoreFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val syncCursorsStores = ConcurrentHashMap<String, DataStore<SyncCursors>>()
    private val userPrefsStores = ConcurrentHashMap<String, DataStore<UserPrefs>>()

    fun syncCursors(userId: String): DataStore<SyncCursors> =
        syncCursorsStores.computeIfAbsent(userId) {
            DataStoreFactory.create(
                serializer = SyncCursorsSerializer,
                produceFile = { File(context.filesDir, "datastore/sync_cursors-$userId.pb") },
            )
        }

    fun userPrefs(userId: String): DataStore<UserPrefs> =
        userPrefsStores.computeIfAbsent(userId) {
            DataStoreFactory.create(
                serializer = UserPrefsSerializer,
                produceFile = { File(context.filesDir, "datastore/user_prefs-$userId.pb") },
            )
        }

    // Not per-user, unlike the two stores above -- see consent_state.proto's own doc for why:
    // Onboarding's consent pane runs before there is a signed-in userId to key a file on.
    private val consentStateStore: DataStore<ConsentState> by lazy {
        DataStoreFactory.create(
            serializer = ConsentStateSerializer,
            produceFile = { File(context.filesDir, "datastore/consent_state.pb") },
        )
    }

    fun consentState(): DataStore<ConsentState> = consentStateStore

    // §7.17 step 6: "Wipe the local DB file, DataStore and session." Deliberately does NOT
    // touch consent_state.pb -- that's device-scoped, not this account's, and a later sign-in
    // on the same device shouldn't be forced through the consent pane again just because a
    // DIFFERENT account was deleted from it. Caller (AccountDeletionRepository) is responsible
    // for closing any open DataStore instance for this userId first; deleting a file out from
    // under a live DataStore is the same hazard as deleting a Room file with an open connection.
    fun deleteUserData(userId: String) {
        File(context.filesDir, "datastore/sync_cursors-$userId.pb").delete()
        File(context.filesDir, "datastore/user_prefs-$userId.pb").delete()
    }
}
