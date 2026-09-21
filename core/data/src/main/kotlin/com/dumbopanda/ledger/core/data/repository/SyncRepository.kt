package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.sync.PullOutcome
import com.dumbopanda.ledger.core.data.sync.Puller
import com.dumbopanda.ledger.core.data.sync.PushOutcome
import com.dumbopanda.ledger.core.data.sync.Pusher
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.datastore.LedgerDataStoreFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import com.dumbopanda.ledger.core.network.sync.SyncApi
import javax.inject.Inject
import javax.inject.Singleton

// §5.7's SyncWorker calls sync() -- one pull, then one push (§5.5's "App
// cold start, session present" trigger, reused by every other trigger too).
enum class SyncOutcome { SUCCESS, RETRYABLE, UNAUTHORIZED, FATAL }

@Singleton
class SyncRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val ledgerDatabaseFactory: LedgerDatabaseFactory,
    private val dataStoreFactory: LedgerDataStoreFactory,
    private val syncApi: SyncApi,
    private val authRepository: AuthRepository,
    private val clock: LedgerClock,
) {
    suspend fun sync(): SyncOutcome {
        val userId = sessionStore.read()?.userId ?: return SyncOutcome.UNAUTHORIZED
        val db = ledgerDatabaseFactory.open(userId)
        try {
            val cursorStore = dataStoreFactory.syncCursors(userId)

            val puller = Puller(db, cursorStore, syncApi, authRepository)
            when (puller.pull()) {
                PullOutcome.SESSION_EXPIRED -> return SyncOutcome.UNAUTHORIZED
                PullOutcome.RETRYABLE -> return SyncOutcome.RETRYABLE
                PullOutcome.SUCCESS -> Unit
            }

            val pusher = Pusher(
                db.pendingOpDao(),
                db.deadLetterDao(),
                db.entryDao(),
                db.accountDao(),
                syncApi,
                authRepository,
                clock,
            )
            return when (pusher.push()) {
                PushOutcome.SUCCESS -> SyncOutcome.SUCCESS
                PushOutcome.RETRYABLE -> SyncOutcome.RETRYABLE
                PushOutcome.SESSION_EXPIRED -> SyncOutcome.UNAUTHORIZED
            }
        } catch (e: Exception) {
            // A bug here must not become an infinite silent WorkManager
            // retry loop -- surface it as Fatal so doWork() gives up cleanly.
            Log.e(TAG, "sync() failed unexpectedly", e)
            return SyncOutcome.FATAL
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TAG = "SyncRepository"
    }
}
