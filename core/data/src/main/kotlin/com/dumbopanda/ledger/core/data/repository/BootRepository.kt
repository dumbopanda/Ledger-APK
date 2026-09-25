package com.dumbopanda.ledger.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place §7.1's six-row table meets real IO -- gathers the impure inputs
 * ([SessionStore], a live refresh attempt, connectivity, the local database, `app_config`) and
 * hands them to [decideBootRoute], the pure function that actually picks a row. Every input is
 * gathered defensively (forced-update and "has local data" both fail closed to "don't know,
 * proceed" -- see their own call sites below) so a boot-time bug in one check can't turn into
 * every user being locked out.
 */
@Singleton
class BootRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val appConfigRepository: AppConfigRepository,
    private val ledgerDatabaseFactory: LedgerDatabaseFactory,
    @ApplicationContext private val context: Context,
) {
    suspend fun decide(): BootRoute {
        val session = sessionStore.read()
        val isOnline = connectivityChecker.isOnline()

        val forcedUpdateRequired = if (isOnline) isForcedUpdateRequired() else false

        if (session == null) {
            return decideBootRoute(
                hasStoredSession = false,
                forcedUpdateRequired = forcedUpdateRequired,
                isOnline = isOnline,
                refreshSucceeded = false,
                hasLocalData = false,
            )
        }

        // Not attempted at all when offline: a network call that can only ever fail offline
        // would just add a timeout to every offline boot for no information gained (§7.1's own
        // offline row doesn't distinguish "refresh failed" from "refresh wasn't reachable").
        val refreshSucceeded = if (isOnline) authRepository.refreshSession() else false

        val hasLocalData = if (isOnline && refreshSucceeded) hasAnyLiveEntries(session.userId) else false

        return decideBootRoute(
            hasStoredSession = true,
            forcedUpdateRequired = forcedUpdateRequired,
            isOnline = isOnline,
            refreshSucceeded = refreshSucceeded,
            hasLocalData = hasLocalData,
        )
    }

    private suspend fun isForcedUpdateRequired(): Boolean {
        val minSupportedBuild = appConfigRepository.fetchMinSupportedBuild() ?: return false
        return installedVersionCode() < minSupportedBuild
    }

    private fun installedVersionCode(): Int = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        info.versionCode
    }.getOrDefault(Int.MAX_VALUE) // unknown -> never forced-update ourselves out of existence

    private suspend fun hasAnyLiveEntries(userId: String): Boolean {
        val db = ledgerDatabaseFactory.open(userId)
        return try {
            db.entryDao().hasAnyLiveEntries().first()
        } catch (e: Exception) {
            Log.w(TAG, "hasAnyLiveEntries check failed, treating local data as absent", e)
            false
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TAG = "BootRepository"
    }
}
