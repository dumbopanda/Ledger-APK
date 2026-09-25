package com.dumbopanda.ledger.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.backup.BackupRepository
import com.dumbopanda.ledger.core.data.backup.ExportOutcome
import com.dumbopanda.ledger.core.data.backup.ImportOutcome
import com.dumbopanda.ledger.core.data.repository.AuthRepository
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

/** §7.13. Export/import (§7.14) are real; see [SettingsScreen] for the SAF launcher wiring this
 * ViewModel doesn't own. Delete account (C11) is real too, but lives on its own screen/ViewModel
 * (`DeleteAccountRoute`/`DeleteAccountViewModel`) -- this ViewModel only ever navigates there. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val backupRepository: BackupRepository,
    private val authRepository: AuthRepository,
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
    private val deadLetterCount = syncStatusRepository.deadLetterCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val hasEntries = backupRepository.hasAnyEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val lastBackupAt = userPrefsRepository.lastBackupAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val nudgeSnoozedUntil = userPrefsRepository.nudgeSnoozedUntil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var showSignOutGuard = false
    private var signingOut = false
    private var exporting = false
    private var importing = false

    private val _uiState = MutableStateFlow(emptyUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(syncMode, showAmounts, requireBiometric, pendingCount, deadLetterCount) { _, _, _, _, _ -> }
                .collect { publish() }
        }
        viewModelScope.launch {
            combine(hasEntries, lastBackupAt, nudgeSnoozedUntil) { _, _, _ -> }.collect { publish() }
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
            SettingsAction.NudgeSnoozed -> viewModelScope.launch { userPrefsRepository.setNudgeSnoozedUntil(clock.now().plusSeconds(SNOOZE_SECONDS)) }
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
            val message = when (val outcome = backupRepository.exportTo(uri)) {
                is ExportOutcome.Success -> "Backup saved -- ${outcome.accountCount} accounts, ${outcome.entryCount} entries"
                ExportOutcome.NotSignedIn -> "Sign in before backing up"
                ExportOutcome.WriteError -> "Couldn't write the backup file"
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
                is ImportOutcome.Success ->
                    if (outcome.accountsAdded == 0 && outcome.entriesAdded == 0) {
                        "Nothing new to import -- already up to date"
                    } else {
                        "Imported ${outcome.accountsAdded} accounts, ${outcome.entriesAdded} entries"
                    }
                is ImportOutcome.UserMismatch -> "That backup belongs to a different account"
                ImportOutcome.NotSignedIn -> "Sign in before importing"
                ImportOutcome.InvalidFile -> "That doesn't look like a ledger backup file"
                ImportOutcome.ReadError -> "Couldn't read that file"
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
        val now = clock.now().atZone(ZoneId.systemDefault())
        _uiState.value = SettingsUiState(
            syncMode = syncMode.value,
            pendingOpCount = pendingCount.value,
            deadLetterCount = deadLetterCount.value,
            showAmountsInAppSwitcher = showAmounts.value,
            requireBiometricToOpen = requireBiometric.value,
            signedInEmail = authRepository.currentEmail(),
            showSignOutGuard = showSignOutGuard,
            signingOut = signingOut,
            exporting = exporting,
            importing = importing,
            showNightlyNudge = shouldShowNightlyNudge(now, hasEntries.value, lastBackupAt.value, nudgeSnoozedUntil.value),
        )
    }

    private fun emptyUiState() = SettingsUiState(
        syncMode = SyncMode.SMART,
        pendingOpCount = 0,
        deadLetterCount = 0,
        showAmountsInAppSwitcher = false,
        requireBiometricToOpen = false,
        signedInEmail = null,
        showSignOutGuard = false,
        signingOut = false,
        exporting = false,
        importing = false,
        showNightlyNudge = false,
    )

    private companion object {
        const val SNOOZE_SECONDS = 4 * 60 * 60L // 4 hours -- long enough to not nag again same night
    }
}
