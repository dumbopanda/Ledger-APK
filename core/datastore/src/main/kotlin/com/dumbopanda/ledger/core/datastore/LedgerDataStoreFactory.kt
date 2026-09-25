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

    // Not per-user, unlike the two stores above -- see consent_state.proto's own doc for why:
    // Onboarding's consent pane runs before there is a signed-in userId to key a file on. Same
    // fresh-instance-per-call contract as syncCursors/userPrefs: short-lived, sequential use.
    fun consentState(): DataStore<ConsentState> = DataStoreFactory.create(
        serializer = ConsentStateSerializer,
        produceFile = { File(context.filesDir, "datastore/consent_state.pb") },
    )

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
