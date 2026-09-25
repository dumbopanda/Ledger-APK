package com.dumbopanda.ledger.feature.entry

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerBottomSheet
import com.dumbopanda.ledger.core.designsystem.component.LedgerCard
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerTextField
import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.DateFieldState
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.feature.entry.component.AccountChips
import com.dumbopanda.ledger.feature.entry.component.AmountCard
import com.dumbopanda.ledger.feature.entry.component.DateField
import com.dumbopanda.ledger.feature.entry.component.SelfTransferToggle
import com.dumbopanda.ledger.feature.entry.component.TransferAccountChips
import com.dumbopanda.ledger.feature.entry.component.TxnTypeCombo
import com.dumbopanda.ledger.feature.entry.component.WillGetBackField
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BANNER_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * §7.5's Add Entry form, plus §7.6's edit mode -- "the same form does both jobs" (one set of
 * fields, one set of rules), which [EntryViewModel] mirrors by branching on whether it's
 * currently editing rather than keeping two code paths. Field order per `ui-spec.md`: Amount
 * → Account(s) → Category → Transaction name → Note → Date → "Will I get this back?" → Save.
 */
@Composable
fun EntryRoute(viewModel: EntryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
    EntryScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
internal fun EntryScreen(uiState: EntryUiState, onAction: (EntryAction) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            EntryUiState.Loading -> Loading()
            EntryUiState.NoAccounts -> NoAccounts()
            EntryUiState.Saved -> Saved()
            is EntryUiState.Ready -> EntryForm(uiState, onAction)
        }
    }
}

@Composable
private fun Loading() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoAccounts() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Add an account first", style = MaterialTheme.typography.titleMedium)
        Text(
            "There's nowhere to record this until at least one account exists.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Saved() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Entry saved", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryForm(state: EntryUiState.Ready, onAction: (EntryAction) -> Unit) {
    val draft = state.draft
    val account = state.accounts.find { it.id == draft.accountId } ?: state.accounts.first()
    val toAccount = state.accounts.find { it.id == draft.toAccountId } ?: account

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (state.isEditing) {
            EditBanner(draft, onCancel = { onAction(EntryAction.CancelEdit) })
        }

        AmountCard(
            amount = draft.amount,
            onAmountChange = { onAction(EntryAction.AmountKeystroke(it.clean, it.caret)) },
            direction = draft.direction,
            onDirectionChange = { onAction(EntryAction.DirectionChanged(it)) },
            directionLocked = draft.selfTransfer,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        SelfTransferToggle(
            checked = draft.selfTransfer,
            onCheckedChange = { onAction(EntryAction.SelfTransferToggled(it)) },
            modifier = Modifier.padding(top = 16.dp),
        )

        if (draft.selfTransfer) {
            TransferAccountChips(
                accounts = state.accounts,
                from = account,
                to = toAccount,
                onChange = { from, to ->
                    onAction(EntryAction.AccountChanged(from))
                    onAction(EntryAction.ToAccountChanged(to))
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            AccountChips(
                accounts = state.accounts,
                selected = account,
                onSelect = { onAction(EntryAction.AccountChanged(it)) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (draft.selfTransfer) {
            TxnTypeCombo(
                selectedType = draft.sentLegType,
                onTypeChange = { onAction(EntryAction.SentLegTypeChanged(it)) },
                label = "Sent leg type",
                modifier = Modifier.padding(top = 16.dp),
            )
            TxnTypeCombo(
                selectedType = draft.receivedLegType,
                onTypeChange = { onAction(EntryAction.ReceivedLegTypeChanged(it)) },
                label = "Received leg type",
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            TxnTypeCombo(
                selectedType = draft.txnType,
                onTypeChange = { onAction(EntryAction.TxnTypeChanged(it)) },
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        LedgerTextField(
            value = draft.expenseName,
            onValueChange = { onAction(EntryAction.ExpenseNameChanged(it)) },
            label = "Transaction name",
            modifier = Modifier.padding(top = 16.dp),
        )

        LedgerTextField(
            value = draft.moreInfo.orEmpty(),
            onValueChange = { onAction(EntryAction.MoreInfoChanged(it)) },
            label = "Note (optional)",
            modifier = Modifier.padding(top = 12.dp),
        )

        DateField(
            value = state.dateText,
            onValueChange = { onAction(EntryAction.DateTextChanged(it)) },
            dateState = state.dateFieldState,
            modifier = Modifier.padding(top = 16.dp),
        )

        if (!draft.selfTransfer) {
            WillGetBackField(
                txnId = draft.txnId,
                openItems = state.openItems,
                onTxnIdChange = { onAction(EntryAction.TxnIdChanged(it)) },
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        LedgerPillButton(
            text = if (state.isEditing) "Save Changes" else "Add Entry",
            onClick = { onAction(EntryAction.SaveTapped) },
            enabled = state.blockedMessage == null && !state.saving,
            modifier = Modifier.padding(top = 24.dp),
        )
        if (state.blockedMessage != null) {
            Text(
                text = state.blockedMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
    }

    if (state.showConfirmSheet) {
        LedgerBottomSheet(onDismissRequest = { onAction(EntryAction.ConfirmDismissed) }) {
            Text(if (state.isEditing) "Save these changes?" else "Add this entry?", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.confirmSentence,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            LedgerPillButton(
                text = if (state.isEditing) "Save changes" else "Add entry",
                onClick = { onAction(EntryAction.ConfirmConfirmed) },
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

// §7.6: "A banner above the form: Y3k9d2 · 14 Sep 2026 · ₹4,500.00 + Cancel."
@Composable
private fun EditBanner(draft: EntryDraft, onCancel: () -> Unit) {
    LedgerCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val amountText = "₹" + FormatAmountUseCase().toDisplay(draft.amount.clean.ifBlank { "0" })
            val dateText = draft.resolvedDate?.format(BANNER_DATE_FORMAT).orEmpty()
            Text(
                "${draft.txnId.value} · $dateText · $amountText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Preview
@Composable
private fun EntryScreenPreview() {
    val domestic = Account("1", "client-1-uid", "Domestic", 0, null)
    val international = Account("2", "client-2-uid", "International", 1, null)
    LedgerTheme {
        EntryScreen(
            uiState = EntryUiState.Ready(
                draft = EntryDraft(
                    resolvedDate = java.time.LocalDate.of(2026, 9, 25),
                    expenseName = "",
                    moreInfo = null,
                    selfTransfer = false,
                    accountId = domestic.id,
                    toAccountId = international.id,
                    txnId = com.dumbopanda.ledger.core.model.TxnId("Nabc123"),
                    txnType = "Payment",
                    sentLegType = "",
                    receivedLegType = "",
                    amount = AmountFieldValue("", 0),
                    direction = TxnDirection.Debit,
                    editModeOriginalIsTransfer = null,
                ),
                dateText = androidx.compose.ui.text.input.TextFieldValue("25/09/2026"),
                dateFieldState = DateFieldState.Resolved(java.time.LocalDate.of(2026, 9, 25), yearInferred = false, daysAgo = 0),
                accounts = listOf(domestic, international),
                openItems = emptyList<OwedToMeItem>(),
                blockedMessage = "Add an expense name",
                showConfirmSheet = false,
                confirmSentence = "",
                saving = false,
                isEditing = false,
            ),
            onAction = {},
        )
    }
}
