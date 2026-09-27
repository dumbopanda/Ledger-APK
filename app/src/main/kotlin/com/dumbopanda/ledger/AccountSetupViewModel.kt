package com.dumbopanda.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.domain.AccountNameValidation
import com.dumbopanda.ledger.core.domain.validateAccountName
import com.dumbopanda.ledger.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AccountSetupRow(val id: String = UUID.randomUUID().toString(), val name: String = "", val error: String? = null)

sealed interface AccountSetupUiState {
    data object Checking : AccountSetupUiState
    // A returning user restored from the server already has accounts -- this screen isn't
    // for them, [AccountSetupScreen] forwards straight past it the moment this is observed.
    data object AlreadyHasAccounts : AccountSetupUiState
    data class Ready(val rows: List<AccountSetupRow>, val saving: Boolean) : AccountSetupUiState
    data object Done : AccountSetupUiState
}

/**
 * First-run replacement for the old silent "seed Domestic/International/Cash" default
 * (AccountRepository.SEED_NAMES was every account WE personally use, not a new user's) --
 * runs once, between Restoring and Home, only for an account that comes back from restore
 * with zero accounts of its own.
 */
@HiltViewModel
class AccountSetupViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AccountSetupUiState>(AccountSetupUiState.Checking)
    val uiState: StateFlow<AccountSetupUiState> = _uiState.asStateFlow()

    private var rows = listOf(AccountSetupRow())

    init {
        viewModelScope.launch {
            _uiState.value = if (accountRepository.hasAnyAccount()) {
                AccountSetupUiState.AlreadyHasAccounts
            } else {
                readyState()
            }
        }
    }

    fun onNameChanged(id: String, name: String) {
        rows = rows.map { if (it.id == id) it.copy(name = name, error = null) else it }
        _uiState.value = readyState()
    }

    fun addRow() {
        rows = rows + AccountSetupRow()
        _uiState.value = readyState()
    }

    // A suggestion chip toggles: tapped once it fills the first blank row (or adds one), tapped
    // again it takes that row back out -- leaving one blank row if it was the only one.
    fun useSuggestion(name: String) {
        val existing = rows.firstOrNull { it.name.trim().equals(name, ignoreCase = true) }
        val blank = rows.firstOrNull { it.name.isBlank() }
        rows = when {
            existing != null -> rows.filterNot { it.id == existing.id }.ifEmpty { listOf(AccountSetupRow()) }
            blank != null -> rows.map { if (it.id == blank.id) it.copy(name = name, error = null) else it }
            else -> rows + AccountSetupRow(name = name)
        }
        _uiState.value = readyState()
    }

    fun removeRow(id: String) {
        if (rows.size <= 1) return
        rows = rows.filterNot { it.id == id }
        _uiState.value = readyState()
    }

    fun confirm() {
        if ((_uiState.value as? AccountSetupUiState.Ready)?.saving == true) return
        if (rows.none { it.name.isNotBlank() }) {
            val firstId = rows.first().id
            rows = rows.map { if (it.id == firstId) it.copy(error = "Add at least one account") else it }
            _uiState.value = readyState()
            return
        }

        var hasError = false
        rows = rows.mapIndexed { index, row ->
            if (row.name.isBlank()) return@mapIndexed row.copy(error = null)
            val others = rows.filterIndexed { i, r -> i != index && r.name.isNotBlank() }.map { it.name }
            when (val validation = validateAccountName(row.name, others)) {
                is AccountNameValidation.Invalid -> { hasError = true; row.copy(error = validation.message) }
                AccountNameValidation.Valid -> row.copy(error = null)
            }
        }
        if (hasError) {
            _uiState.value = readyState()
            return
        }

        val toCreate = rows.filter { it.name.isNotBlank() }
        _uiState.value = AccountSetupUiState.Ready(rows, saving = true)
        viewModelScope.launch {
            toCreate.forEachIndexed { index, row ->
                accountRepository.addAccount(
                    Account(
                        id = UUID.randomUUID().toString(),
                        clientUid = UUID.randomUUID().toString(),
                        name = row.name.trim(),
                        sortOrder = index,
                        archivedAt = null,
                    ),
                )
            }
            _uiState.value = AccountSetupUiState.Done
        }
    }

    private fun readyState() = AccountSetupUiState.Ready(rows, saving = false)
}
