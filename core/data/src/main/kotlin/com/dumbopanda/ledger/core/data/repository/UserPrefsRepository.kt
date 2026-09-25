package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.data.sync.SyncMode
import com.dumbopanda.ledger.core.datastore.LedgerDataStoreFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import com.dumbopanda.ledger.core.datastore.UserPrefs
import com.dumbopanda.ledger.core.datastore.UserPrefsSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.dumbopanda.ledger.core.datastore.SyncMode as ProtoSyncMode

/**
 * §7.13's Sync mode and Security toggles -- the one reader/writer of the per-user `UserPrefs`
 * proto DataStore (§6's own note: not the auth session, which is EncryptedSharedPreferences).
 * A fresh `DataStore` instance per call, same as [SyncRepository]'s own `syncCursors` usage --
 * short-lived, sequential, never held open concurrently with another instance for the same
 * file, which is the actual hazard DataStore's single-instance guidance warns about.
 */
@Singleton
class UserPrefsRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val dataStoreFactory: LedgerDataStoreFactory,
) {
    fun syncMode(): Flow<SyncMode> = prefs().map { it.syncMode.toDomain() }
    fun showAmountsInAppSwitcher(): Flow<Boolean> = prefs().map { it.showAmountsInAppSwitcher }
    fun requireBiometricToOpen(): Flow<Boolean> = prefs().map { it.requireBiometricToOpen }

    suspend fun setSyncMode(mode: SyncMode) = update { it.setSyncMode(mode.toProto()) }
    suspend fun setShowAmountsInAppSwitcher(show: Boolean) = update { it.setShowAmountsInAppSwitcher(show) }
    suspend fun setRequireBiometricToOpen(require: Boolean) = update { it.setRequireBiometricToOpen(require) }

    // For a write path that needs the CURRENT mode once (EntryRepository/AccountRepository,
    // deciding how to notify SyncScheduler right after enqueueing an op) without holding a
    // live collector open for the rest of that repository's lifetime.
    suspend fun currentSyncMode(): SyncMode = prefs().first().syncMode.toDomain()

    private fun prefs(): Flow<UserPrefs> {
        val userId = sessionStore.read()?.userId ?: return flowOf(UserPrefsSerializer.defaultValue)
        return dataStoreFactory.userPrefs(userId).data
    }

    private suspend fun update(transform: (UserPrefs.Builder) -> UserPrefs.Builder) {
        val userId = sessionStore.read()?.userId ?: return
        dataStoreFactory.userPrefs(userId).updateData { current -> transform(current.toBuilder()).build() }
    }
}

// internal, not private -- UserPrefsRepositoryMappingTest exercises these directly rather than
// round-tripping through a real DataStore file for what is otherwise pure enum mapping.
internal fun ProtoSyncMode.toDomain(): SyncMode = when (this) {
    ProtoSyncMode.SYNC_MODE_INSTANT -> SyncMode.INSTANT
    ProtoSyncMode.SYNC_MODE_MANUAL -> SyncMode.MANUAL
    ProtoSyncMode.SYNC_MODE_SMART, ProtoSyncMode.SYNC_MODE_UNSPECIFIED, ProtoSyncMode.UNRECOGNIZED -> SyncMode.SMART
}

internal fun SyncMode.toProto(): ProtoSyncMode = when (this) {
    SyncMode.SMART -> ProtoSyncMode.SYNC_MODE_SMART
    SyncMode.INSTANT -> ProtoSyncMode.SYNC_MODE_INSTANT
    SyncMode.MANUAL -> ProtoSyncMode.SYNC_MODE_MANUAL
}
