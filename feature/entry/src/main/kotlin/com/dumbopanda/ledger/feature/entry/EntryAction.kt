package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.input.TextFieldValue
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId

sealed interface EntryAction {
    data class DateTextChanged(val value: TextFieldValue) : EntryAction
    data class ExpenseNameChanged(val value: String) : EntryAction
    data class MoreInfoChanged(val value: String) : EntryAction
    data class SelfTransferToggled(val on: Boolean) : EntryAction
    data class AccountChanged(val account: Account) : EntryAction
    data class ToAccountChanged(val account: Account) : EntryAction
    data class TxnTypeChanged(val value: String) : EntryAction
    data class SentLegTypeChanged(val value: String) : EntryAction
    data class ReceivedLegTypeChanged(val value: String) : EntryAction
    data class AmountKeystroke(val rawText: String, val rawCaret: Int) : EntryAction
    data class DirectionChanged(val direction: TxnDirection) : EntryAction
    data class TxnIdChanged(val txnId: TxnId) : EntryAction
    data object SaveTapped : EntryAction
    data object ConfirmConfirmed : EntryAction
    data object ConfirmDismissed : EntryAction
    // §7.6: resets the form completely and toasts "Edit cancelled — nothing changed".
    data object CancelEdit : EntryAction
}
