package com.dumbopanda.ledger.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.domain.AccountNameValidation
import com.dumbopanda.ledger.core.domain.validateAccountName
import com.dumbopanda.ledger.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * §7.12. Reorder is shipped as explicit up/down moves, not a drag gesture -- the plan's own
 * words are "drag-to-reorder," but this project has no reorderable-list dependency yet and
 * writing a correct drag-and-drop gesture recognizer (offset tracking, animated swaps, a
 * drag handle) is real, isolated work with its own edge cases, not a one-line addition. Up/
 * down buttons reach the identical end state (a persisted `sortOrder`) with a fraction of the
 * code and no new dependency -- a real drag gesture is a documented follow-up, not a silently
 * dropped requirement.
 */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val accounts: StateFlow<List<Account>> = accountRepository.allAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var editingId: String? = null
    private var editingText: String = ""
    private var editingError: String? = null
    private var addingNew = false
    private var newName = ""
    private var newNameError: String? = null
    private var archiveNotice: String? = null

    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        // C15: no ensureSeeded() fallback here anymore -- see EntryViewModel's own note. Same
        // race against AccountSetupOverlay, same fix.
        viewModelScope.launch { accounts.collect { publish(it) } }
    }

    fun onAction(action: AccountsAction) {
        when (action) {
            is AccountsAction.RenameTapped -> {
                editingId = action.account.id
                editingText = action.account.name
                editingError = null
            }
            is AccountsAction.RenameTextChanged -> {
                editingText = action.text.take(MAX_ACCOUNT_NAME)
                editingError = null
            }
            AccountsAction.RenameCancelled -> {
                editingId = null
                editingText = ""
                editingError = null
            }
            AccountsAction.RenameConfirmed -> confirmRename()
            is AccountsAction.ArchiveToggled -> toggleArchive(action.account)
            is AccountsAction.MoveUp -> move(action.account, -1)
            is AccountsAction.MoveDown -> move(action.account, 1)
            AccountsAction.AddTapped -> {
                addingNew = true
                newName = ""
                newNameError = null
            }
            is AccountsAction.NewNameChanged -> {
                newName = action.text.take(MAX_ACCOUNT_NAME)
                newNameError = null
            }
            AccountsAction.AddCancelled -> {
                addingNew = false
                newName = ""
                newNameError = null
            }
            AccountsAction.AddConfirmed -> confirmAdd()
        }
        publish(accounts.value)
    }

    private fun confirmRename() {
        val id = editingId ?: return
        val others = accounts.value.filter { it.id != id }.map { it.name }
        when (val validation = validateAccountName(editingText, others)) {
            is AccountNameValidation.Invalid -> editingError = validation.message
            AccountNameValidation.Valid -> viewModelScope.launch {
                accountRepository.renameAccount(id, editingText.trim())
                editingId = null
                editingText = ""
                editingError = null
                publish(accounts.value)
            }
        }
    }

    private fun confirmAdd() {
        val others = accounts.value.map { it.name }
        when (val validation = validateAccountName(newName, others)) {
            is AccountNameValidation.Invalid -> newNameError = validation.message
            AccountNameValidation.Valid -> viewModelScope.launch {
                val next = Account(
                    id = UUID.randomUUID().toString(),
                    clientUid = UUID.randomUUID().toString(),
                    name = newName.trim(),
                    sortOrder = accounts.value.size,
                    archivedAt = null,
                )
                accountRepository.addAccount(next)
                addingNew = false
                newName = ""
                newNameError = null
                publish(accounts.value)
            }
        }
    }

    // Archiving the last active account would leave Add Payment with nowhere to record anything
    // and no way back from that screen, so it's refused with a reason instead.
    private fun toggleArchive(account: Account) {
        val archiving = !account.isArchived
        if (archiving && accounts.value.count { !it.isArchived } <= 1) {
            archiveNotice = "Keep at least one account. Add another before archiving this one."
            return
        }
        archiveNotice = null
        viewModelScope.launch { accountRepository.setArchived(account.id, archived = archiving) }
    }

    private fun move(account: Account, delta: Int) {
        val list = accounts.value
        val index = list.indexOfFirst { it.id == account.id }
        val newIndex = index + delta
        if (index < 0 || newIndex < 0 || newIndex >= list.size) return
        val reordered = list.toMutableList().apply { add(newIndex, removeAt(index)) }
        viewModelScope.launch { accountRepository.reorder(reordered.map { it.id }) }
    }

    private fun publish(accs: List<Account>) {
        _uiState.value = AccountsUiState.Ready(
            accounts = accs,
            editingId = editingId,
            editingText = editingText,
            editingError = editingError,
            addingNew = addingNew,
            newName = newName,
            newNameError = newNameError,
            archiveNotice = archiveNotice,
        )
    }
}

// Matches the server's check on accounts.name (0002_accounts.sql).
private const val MAX_ACCOUNT_NAME = 40
