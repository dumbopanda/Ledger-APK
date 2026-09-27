package com.dumbopanda.ledger.feature.accounts

import com.dumbopanda.ledger.core.model.Account

sealed interface AccountsUiState {
    data object Loading : AccountsUiState

    data class Ready(
        // Ordered by sortOrder, archived included (§7.12 shows them, dimmed, so they can be
        // unarchived -- only the chip pickers elsewhere hide them).
        val accounts: List<Account>,
        val editingId: String?,
        val editingText: String,
        val editingError: String?,
        val addingNew: Boolean,
        val newName: String,
        val newNameError: String?,
        val archiveNotice: String? = null,
        // Signed opening balance per account id (negative = owed on it, e.g. a credit card).
        val openingBalances: Map<String, Long> = emptyMap(),
        val balanceEditingId: String? = null,
        val balanceText: String = "",
        val balanceOwed: Boolean = false,
    ) : AccountsUiState
}
