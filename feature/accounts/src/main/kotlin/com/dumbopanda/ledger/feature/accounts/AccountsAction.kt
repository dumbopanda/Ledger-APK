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

    // Opening balance: what was in the account (or owed on it) when you started with Ledger.
    data class BalanceTapped(val account: Account) : AccountsAction
    data class BalanceTextChanged(val text: String) : AccountsAction
    data class BalanceOwedChanged(val owed: Boolean) : AccountsAction
    data object BalanceConfirmed : AccountsAction
    data object BalanceCancelled : AccountsAction
}
