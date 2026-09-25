package com.dumbopanda.ledger.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AuthRepository
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
import javax.inject.Inject

/** §7.13. Backup (C8) and Delete account (C11) rows are present but stubbed -- neither block
 * exists yet; see [SettingsScreen] for exactly which rows that applies to. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val authRepository: AuthRepository,
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

    private var showSignOutGuard = false
    private var signingOut = false

    private val _uiState = MutableStateFlow(emptyUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(syncMode, showAmounts, requireBiometric, pendingCount, deadLetterCount) { a, b, c, d, e -> Quint(a, b, c, d, e) }
                .collect { publish() }
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
            SettingsAction.SignOutTapped -> {
                if (pendingCount.value > 0) showSignOutGuard = true else signOut()
            }
            SettingsAction.SignOutGuardDismissed -> showSignOutGuard = false
            SettingsAction.SendPendingThenSignOutTapped -> {
                syncStatusRepository.sendNow()
                showSignOutGuard = false
            }
            SettingsAction.SignOutConfirmed -> signOut()
            SettingsAction.DeleteAccountTapped -> Unit // C11
        }
        publish()
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
        _uiState.value = SettingsUiState(
            syncMode = syncMode.value,
            pendingOpCount = pendingCount.value,
            deadLetterCount = deadLetterCount.value,
            showAmountsInAppSwitcher = showAmounts.value,
            requireBiometricToOpen = requireBiometric.value,
            signedInEmail = authRepository.currentEmail(),
            showSignOutGuard = showSignOutGuard,
            signingOut = signingOut,
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
    )

    private data class Quint(val a: SyncMode, val b: Boolean, val c: Boolean, val d: Int, val e: Int)
}
