package com.dumbopanda.ledger.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// §5.6's three modes, as in the prototype.
enum class SyncMode { SMART, INSTANT, MANUAL }

// §5.5/§5.6: the six trigger points, translated into WorkManager enqueues.
// Every enqueue below carries the same NetworkType.CONNECTED constraint, so
// "connectivity regained" (trigger 4) needs no separate action here --
// WorkManager itself releases whatever is already queued once the network
// returns.
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val ledgerDatabaseFactory: LedgerDatabaseFactory,
    private val clock: LedgerClock,
) {
    private val workManager get() = WorkManager.getInstance(context)

    // Trigger 1: app cold start, session present -- pull then push now.
    fun onColdStart() = enqueueNow()

    // Trigger 2: app foregrounded. The ">60s since last pull" check (§5.5)
    // is the caller's job (it owns the last-pull timestamp); by the time
    // this is called, that decision has already been made.
    fun onForegrounded() = enqueueNow()

    // Trigger 3: an entry/account op was just enqueued to the op log.
    suspend fun onOpEnqueued(mode: SyncMode) {
        when (mode) {
            SyncMode.INSTANT -> enqueueNow()
            SyncMode.MANUAL -> Unit
            SyncMode.SMART -> enqueueSmart()
        }
    }

    // Trigger 5: "Send everything now" -- ignores the settle timer.
    fun onManualSyncRequested() = enqueueNow()

    // Trigger 6: session restored after SessionExpired (re-sign-in or a
    // refresh that finally succeeded) -- ops accumulated while signed out.
    fun onSessionRestored() = enqueueNow()

    private fun enqueueNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    // Settle restarts on every new op (REPLACE) so a burst goes out as one
    // call; the ceiling is measured from the OLDEST queued op and never
    // pushed back, so a lone entry waits for company but not all day. Once
    // BATCH_FULL ops are already queued, waiting buys nothing -- go now.
    private suspend fun enqueueSmart() {
        val userId = sessionStore.read()?.userId ?: return
        val db = ledgerDatabaseFactory.open(userId)
        try {
            val pending = db.pendingOpDao().page(BATCH_FULL)
            if (pending.size >= BATCH_FULL) {
                enqueueNow()
                return
            }
            val oldest = pending.firstOrNull() ?: return
            val now = clock.now()
            val fireAt = minOf(now.plusSeconds(SETTLE_SECONDS), oldest.createdAt.plusSeconds(CEILING_SECONDS))
            val delaySeconds = maxOf(0L, fireAt.epochSecond - now.epochSecond)

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_SECONDS, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        } finally {
            db.close()
        }
    }

    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val UNIQUE_WORK_NAME = "sync"
        const val BATCH_FULL = 3
        const val SETTLE_SECONDS = 90L
        const val CEILING_SECONDS = 3600L
        const val RETRY_SECONDS = 30L
    }
}
