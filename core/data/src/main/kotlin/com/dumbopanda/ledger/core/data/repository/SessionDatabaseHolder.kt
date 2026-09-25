package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The long-lived, reactive counterpart to [SyncRepository]'s open-per-call pattern.
 * [SyncRepository] opens and closes its own connection for one background sync; anything the
 * UI observes continuously (a `PagingSource`, `openYIds()`, ...) needs ONE connection that
 * outlives a single call, which this holds. [LedgerDatabaseFactory]'s own doc comment says the
 * caller is responsible for closing the previous user's instance before opening the next --
 * this class is that caller, the one place in the app that does.
 *
 * Not reactive to [SessionStore] itself (that store has no Flow -- sign-in/out are explicit
 * user actions here, not ambient state to observe): [database] is called lazily by repository
 * methods, and re-opens only when the signed-in user actually changed since the last call.
 */
@Singleton
class SessionDatabaseHolder @Inject constructor(
    private val factory: LedgerDatabaseFactory,
    private val sessionStore: SessionStore,
) {
    @Volatile private var opened: LedgerDatabase? = null
    @Volatile private var openedForUserId: String? = null

    @Synchronized
    fun database(): LedgerDatabase {
        val userId = sessionStore.read()?.userId ?: error("SessionDatabaseHolder.database() called with no active session")
        return database(userId)
    }

    // internal, not private -- SessionDatabaseHolderTest (C10's "sign in as A, then B, then A"
    // gate) drives the actual reopen/close decision directly with plain userIds, the same
    // reasoning UserPrefsRepositoryMappingTest uses to exercise toDomain()/toProto() without a
    // real DataStore file: SessionStore is EncryptedSharedPreferences and needs a real Android
    // Keystore, which Robolectric doesn't provide, so a test can't drive this through
    // sessionStore.read() the way production code does.
    @Synchronized
    internal fun database(userId: String): LedgerDatabase {
        opened?.let { if (openedForUserId == userId) return it }
        opened?.close()
        val db = factory.open(userId)
        opened = db
        openedForUserId = userId
        return db
    }

    /** Sign-out: drop the connection so the next [database] call (a fresh sign-in) opens clean. */
    @Synchronized
    fun close() {
        opened?.close()
        opened = null
        openedForUserId = null
    }
}
