package com.dumbopanda.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.SyncOutcome
import com.dumbopanda.ledger.core.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// §7.4: "Full pull for a new device or reinstall. Determinate progress from pull_entries page
// counts. No skip. On failure: retry, or 'continue offline'."
@HiltViewModel
class RestoringViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
) : ViewModel() {
    var pulledCount by mutableIntStateOf(0)
        private set
    var failed by mutableStateOf(false)
        private set
    var done by mutableStateOf(false)
        private set

    init {
        restore()
    }

    fun retry() {
        pulledCount = 0
        failed = false
        restore()
    }

    private fun restore() {
        viewModelScope.launch {
            when (syncRepository.restore(onProgress = { pulledCount = it })) {
                SyncOutcome.SUCCESS -> done = true
                SyncOutcome.RETRYABLE, SyncOutcome.UNAUTHORIZED, SyncOutcome.FATAL -> failed = true
            }
        }
    }
}
