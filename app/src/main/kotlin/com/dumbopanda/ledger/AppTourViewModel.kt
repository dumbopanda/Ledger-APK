package com.dumbopanda.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val APP_TOUR_PANE_COUNT = 4

sealed interface AppTourUiState {
    data object Checking : AppTourUiState

    // A returning account (already has accounts, from a second device or a reinstall) has
    // already seen this -- same "ask the server/DB, don't guess" skip AccountSetup uses, and
    // for the same reason: re-showing a tour to someone who already knows the app is a worse
    // first impression than the empty screen it's meant to prevent.
    data object AlreadySeen : AppTourUiState

    data class Ready(val pane: Int) : AppTourUiState
    data object Done : AppTourUiState
}

/**
 * Runs once, between Profile setup and Account setup, only for a brand-new account -- same
 * once-per-new-account gate as [AccountSetupViewModel] ([AccountRepository.hasAnyAccount]), and
 * deliberately no separate "tour seen" flag: that gate already means exactly the same thing a
 * dedicated one would.
 *
 * The owner's own instruction (2026-09-27): show the tour BEFORE account setup, not after, so a
 * new user isn't asked to name accounts before they know what any of this is for. Since no
 * accounts exist yet, each pane is a crafted mockup of its real screen -- not a live overlay --
 * same "show the product, don't just describe it" approach Onboarding's own day-card visual
 * already uses.
 */
@HiltViewModel
class AppTourViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppTourUiState>(AppTourUiState.Checking)
    val uiState: StateFlow<AppTourUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = if (accountRepository.hasAnyAccount()) AppTourUiState.AlreadySeen else AppTourUiState.Ready(0)
        }
    }

    fun next() {
        val state = _uiState.value as? AppTourUiState.Ready ?: return
        _uiState.value = if (state.pane + 1 >= APP_TOUR_PANE_COUNT) AppTourUiState.Done else state.copy(pane = state.pane + 1)
    }

    fun back() {
        val state = _uiState.value as? AppTourUiState.Ready ?: return
        if (state.pane > 0) _uiState.value = state.copy(pane = state.pane - 1)
    }

    fun skip() {
        _uiState.value = AppTourUiState.Done
    }
}
