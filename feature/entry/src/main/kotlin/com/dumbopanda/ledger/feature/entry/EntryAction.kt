package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.input.TextFieldValue
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId

sealed interface EntryAction {
    data class DateTextChanged(val value: TextFieldValue) : EntryAction
    data object DateBlurred : EntryAction
    data class DateQuickPicked(val quick: QuickDate) : EntryAction

    // Picking a known name from the list ("" is the "Choose a name…" row).
    data class ExpenseNameChosen(val name: String) : EntryAction
    data object AddNewNameChosen : EntryAction
    data class ExpenseNameTyped(val value: String) : EntryAction

    data class MoreInfoChanged(val value: String) : EntryAction
    data class SelfTransferToggled(val on: Boolean) : EntryAction
    data class AccountChanged(val account: Account) : EntryAction
    data class ToAccountChanged(val account: Account) : EntryAction
    data class TxnTypeChanged(val value: String) : EntryAction
    data class SentLegTypeChanged(val value: String) : EntryAction
    data class ReceivedLegTypeChanged(val value: String) : EntryAction
    data class AmountKeystroke(val rawText: String, val rawCaret: Int) : EntryAction
    data object AmountBlurred : EntryAction
    data class DirectionChanged(val direction: TxnDirection) : EntryAction

    // "Will I get this back?" -- Yes/No, then "Which one" (New/Old), then "Which open item".
    data class BackChanged(val yes: Boolean) : EntryAction
    data class BackKindChanged(val old: Boolean) : EntryAction
    data class OpenItemChosen(val txnId: TxnId) : EntryAction

    data object SaveTapped : EntryAction
    data object ConfirmConfirmed : EntryAction
    data object ConfirmDismissed : EntryAction
    data object CancelEdit : EntryAction
}
