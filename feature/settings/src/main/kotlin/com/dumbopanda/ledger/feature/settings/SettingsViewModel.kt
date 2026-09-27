package com.dumbopanda.ledger.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.backup.BackupRepository
import com.dumbopanda.ledger.core.data.backup.ExportOutcome
import com.dumbopanda.ledger.core.data.backup.ImportOutcome
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.data.repository.ConnectivityChecker
import com.dumbopanda.ledger.core.data.repository.SyncStatusRepository
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.dumbopanda.ledger.core.data.sync.SyncMode
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// Batched mode's own rules (SyncScheduler): 3+ go together, never held past an hour.
private const val BATCH_FULL = 3
private const val CEILING_SECONDS = 3600L
private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
private val STAMP = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK)

/**
 * The Settings screen behind the profile button -- index.html's account screen (waiting to be
 * sent, when to send, backup) plus what only the APK needs (accounts, security, sign-in).
 * Export/import pick their files in the Composable; this only ever sees the resulting Uri.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val backupRepository: BackupRepository,
    private val authRepository: AuthRepository,
    private val connectivityChecker: ConnectivityChecker,
    accountRepository: AccountRepository,
    private val clock: LedgerClock,
) : ViewModel() {

    private val syncMode = userPrefsRepository.syncMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncMode.SMART)
    private val showAmounts = userPrefsRepository.showAmountsInAppSwitcher()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val requireBiometric = userPrefsRepository.requireBiometricToOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val pendingCount = syncStatusRepository.pendingOpCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val oldestPending = syncStatusRepository.oldestPendingAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val deadLetterCount = syncStatusRepository.deadLetterCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val entryCount = syncStatusRepository.liveEntryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val lastBackupAt = userPrefsRepository.lastBackupAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val accounts = accountRepository.activeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val online = connectivityChecker.onlineFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), connectivityChecker.isOnline())

    private var showSignOutGuard = false
    private var signingOut = false
    private var exporting = false
    private var importing = false

    private val _uiState = MutableStateFlow(build())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(syncMode, showAmounts, requireBiometric, pendingCount, oldestPending) { _, _, _, _, _ -> }.collect { publish() }
        }
        viewModelScope.launch {
            combine(deadLetterCount, entryCount, lastBackupAt, accounts, online) { _, _, _, _, _ -> }.collect { publish() }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SyncModeChanged -> viewModelScope.launch { userPrefsRepository.setSyncMode(action.mode) }
            SettingsAction.SendNowTapped -> syncStatusRepository.sendNow()
            is SettingsAction.ShowAmountsToggled ->
                viewModelScope.launch { userPrefsRepository.setShowAmountsInAppSwitcher(action.show) }
            is SettingsAction.RequireBiometricToggled ->
                viewModelScope.launch { userPrefsRepository.setRequireBiometricToOpen(action.require) }
            is SettingsAction.ExportRequested -> export(action.uri)
            is SettingsAction.ImportRequested -> import(action.uri)
            SettingsAction.SignOutTapped -> {
                if (pendingCount.value > 0) showSignOutGuard = true else signOut()
            }
            SettingsAction.SignOutGuardDismissed -> showSignOutGuard = false
            SettingsAction.SendPendingThenSignOutTapped -> {
                syncStatusRepository.sendNow()
                showSignOutGuard = false
            }
            SettingsAction.SignOutConfirmed -> signOut()
        }
        publish()
    }

    private fun export(uri: Uri) {
        exporting = true
        publish()
        viewModelScope.launch {
            val message = when (backupRepository.exportTo(uri)) {
                is ExportOutcome.Success -> "File saved"
                ExportOutcome.NotSignedIn -> "Sign in before saving a file."
                ExportOutcome.WriteError -> "That file couldn't be saved."
            }
            exporting = false
            _events.tryEmit(message)
            publish()
        }
    }

    private fun import(uri: Uri) {
        importing = true
        publish()
        viewModelScope.launch {
            val message = when (val outcome = backupRepository.importFrom(uri)) {
                is ImportOutcome.Success -> {
                    val skipped = if (outcome.skipped > 0) " Skipped ${outcome.skipped} that didn't look right." else ""
                    if (outcome.accountsAdded == 0 && outcome.entriesAdded == 0) {
                        "Everything in that file was already here.$skipped"
                    } else {
                        "Restored ${outcome.entriesAdded} ${if (outcome.entriesAdded == 1) "entry" else "entries"}.$skipped"
                    }
                }
                is ImportOutcome.UserMismatch -> "That file belongs to a different account."
                ImportOutcome.NotSignedIn -> "Sign in before restoring."
                ImportOutcome.InvalidFile -> "That isn't a Ledger file, or it's damaged."
                ImportOutcome.ReadError -> "That file couldn't be opened."
            }
            importing = false
            _events.tryEmit(message)
            publish()
        }
    }

    private fun signOut() {
        signingOut = true
        showSignOutGuard = false
        publish()
        viewModelScope.launch {
            authRepository.signOut()
            signingOut = false
            _signedOut.tryEmit(Unit)
        }
    }

    private fun publish() {
        _uiState.value = build()
    }

    // Every flow read here is declared above `_uiState`, so all are initialised by the time
    // its initialiser first calls this.
    private fun build(): SettingsUiState {
        val n = pendingCount.value
        val online = this.online.value
        val mode = syncMode.value
        return SettingsUiState(
            syncMode = mode,
            pendingOpCount = n,
            deadLetterCount = deadLetterCount.value,
            queueSummary = queueSummary(n, online, mode),
            sendLabel = if (!online && n > 0) "Offline" else "Back up",
            sendEnabled = n > 0 && online,
            syncModeHint = when (mode) {
                SyncMode.INSTANT -> "Each change goes up the moment you save it."
                SyncMode.MANUAL -> "Nothing leaves this phone until you tap Back up."
                SyncMode.SMART -> "Waits for a pause, then sends a few changes together. Never more than an hour. Easy on battery and data."
            },
            backupStatus = backupStatus(),
            accountCount = accounts.value.size,
            showAmountsInAppSwitcher = showAmounts.value,
            requireBiometricToOpen = requireBiometric.value,
            signedInEmail = authRepository.currentEmail(),
            showSignOutGuard = showSignOutGuard,
            signingOut = signingOut,
            exporting = exporting,
            importing = importing,
        )
    }

    // index.html's whenText().
    private fun queueSummary(n: Int, online: Boolean, mode: SyncMode): String {
        if (n == 0) return "All backed up"
        val count = if (n == 1) "1 change" else "$n changes"
        if (!online) return "$count · sends when you're back online"
        return when {
            mode == SyncMode.MANUAL -> "$count · tap Back up"
            mode == SyncMode.INSTANT -> "$count · trying again"
            n >= BATCH_FULL -> "$count · sending shortly"
            else -> oldestPending.value
                ?.plusSeconds(CEILING_SECONDS)
                ?.atZone(ZoneId.systemDefault())
                ?.let { "$count · sending by ${it.format(TIME)}" }
                ?: "$count · sending soon"
        }
    }

    private fun backupStatus(): String {
        val n = entryCount.value
        val count = if (n == 1) "1 entry" else "$n entries"
        val last = lastBackupAt.value ?: return "$count · no file saved yet"
        return "$count · file saved ${last.atZone(ZoneId.systemDefault()).format(STAMP)}"
    }
}
