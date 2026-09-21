package com.dumbopanda.ledger.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dumbopanda.ledger.core.data.repository.SyncOutcome
import com.dumbopanda.ledger.core.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// §5.7. Enqueued as unique work "sync" by SyncScheduler.
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (syncRepository.sync()) {
        SyncOutcome.SUCCESS -> Result.success()
        SyncOutcome.RETRYABLE -> Result.retry()
        SyncOutcome.UNAUTHORIZED -> Result.success() // SessionExpired owns the UI
        SyncOutcome.FATAL -> Result.failure()
    }
}
