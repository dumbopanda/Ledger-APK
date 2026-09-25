package com.dumbopanda.ledger.feature.accounts

import com.dumbopanda.ledger.core.model.Account

sealed interface AccountsAction {
    data class RenameTapped(val account: Account) : AccountsAction
    data class RenameTextChanged(val text: String) : AccountsAction
    data object RenameConfirmed : AccountsAction
    data object RenameCancelled : AccountsAction
    data class ArchiveToggled(val account: Account) : AccountsAction
    // §7.12's "drag-to-reorder", shipped as explicit up/down moves rather than a drag gesture
    // -- see AccountsViewModel's own doc for why.
    data class MoveUp(val account: Account) : AccountsAction
    data class MoveDown(val account: Account) : AccountsAction
    data object AddTapped : AccountsAction
    data class NewNameChanged(val text: String) : AccountsAction
    data object AddConfirmed : AccountsAction
    data object AddCancelled : AccountsAction
}
