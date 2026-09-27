package com.dumbopanda.ledger

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.backup.BackupRepository
import com.dumbopanda.ledger.core.data.backup.ExportOutcome
import com.dumbopanda.ledger.core.data.repository.ConnectivityChecker
import com.dumbopanda.ledger.core.data.repository.SyncStatusRepository
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.dumbopanda.ledger.core.data.sync.SyncMode
import com.dumbopanda.ledger.core.domain.shouldShowNightlyNudge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    // The lime badge on the profile button, and the sync bar's count.
    val pendingCount: Int,
    val online: Boolean,
    val syncMode: SyncMode,
    // index.html's backup bar -- the nightly "Back up today's entries" nudge.
    val showBackupBar: Boolean,
)

/** What the app shell shows above every screen: the pending badge, the sync and backup bars. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncStatusRepository: SyncStatusRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val backupRepository: BackupRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val clock: LedgerClock,
) : ViewModel() {

    private val pending = syncStatusRepository.pendingOpCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val syncMode = userPrefsRepository.syncMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncMode.SMART)
    private val hasEntries = backupRepository.hasAnyEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val lastBackupAt = userPrefsRepository.lastBackupAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val snoozedUntil = userPrefsRepository.nudgeSnoozedUntil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val online = connectivityChecker.onlineFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), connectivityChecker.isOnline())

    private val _uiState = MutableStateFlow(build())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine<Any?, Unit>(pending, syncMode, hasEntries, lastBackupAt, snoozedUntil, online) { }.collect { _uiState.value = build() }
        }
    }

    fun sendNow() = syncStatusRepository.sendNow()

    // "Later" -- the same 4-hour snooze Settings used to offer.
    fun snoozeBackup() {
        viewModelScope.launch { userPrefsRepository.setNudgeSnoozedUntil(clock.now().plusSeconds(SNOOZE_SECONDS)) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val message = when (backupRepository.exportTo(uri)) {
                is ExportOutcome.Success -> "File saved"
                ExportOutcome.NotSignedIn -> "Sign in before saving a file."
                ExportOutcome.WriteError -> "That file couldn't be saved."
            }
            _events.tryEmit(message)
        }
    }

    // Every flow read here is declared above `_uiState`.
    private fun build() = HomeUiState(
        pendingCount = pending.value,
        online = online.value,
        syncMode = syncMode.value,
        showBackupBar = shouldShowNightlyNudge(
            clock.now().atZone(ZoneId.systemDefault()),
            hasEntries.value,
            lastBackupAt.value,
            snoozedUntil.value,
        ),
    )

    private companion object {
        const val SNOOZE_SECONDS = 4 * 60 * 60L
    }
}
