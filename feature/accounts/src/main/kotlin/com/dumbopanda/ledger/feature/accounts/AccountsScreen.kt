package com.dumbopanda.ledger.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerCard
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerTextField
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.model.Account

/** §7.12: list, rename (inline), archive/unarchive, reorder (up/down -- see
 * [AccountsViewModel]'s own doc), and an add-account row (needed for "N accounts, generalized"
 * to mean anything beyond the three seeded ones; not itself named in §7.12's text but implied
 * by ui-spec.md's own "4+ accounts" tile-collapse rule, which has nowhere else to come from). */
@Composable
fun AccountsRoute(viewModel: AccountsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
internal fun AccountsScreen(uiState: AccountsUiState, onAction: (AccountsAction) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            AccountsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is AccountsUiState.Ready -> AccountsList(uiState, onAction)
        }
    }
}

@Composable
private fun AccountsList(state: AccountsUiState.Ready, onAction: (AccountsAction) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(state.accounts, key = { it.id }) { account ->
            AccountRow(
                account = account,
                isFirst = state.accounts.first() == account,
                isLast = state.accounts.last() == account,
                editing = state.editingId == account.id,
                editingText = state.editingText,
                editingError = state.editingError,
                onAction = onAction,
            )
        }
        item {
            AddAccountRow(state, onAction)
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    isFirst: Boolean,
    isLast: Boolean,
    editing: Boolean,
    editingText: String,
    editingError: String?,
    onAction: (AccountsAction) -> Unit,
) {
    LedgerCard(modifier = Modifier.padding(bottom = 8.dp).alpha(if (account.isArchived) 0.55f else 1f)) {
        if (editing) {
            LedgerTextField(
                value = editingText,
                onValueChange = { onAction(AccountsAction.RenameTextChanged(it)) },
                label = "Account name",
                isError = editingError != null,
                supportingText = editingError,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAction(AccountsAction.RenameCancelled) }) { Text("Cancel") }
                LedgerPillButton(text = "Save", onClick = { onAction(AccountsAction.RenameConfirmed) })
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(account.name, style = MaterialTheme.typography.bodyLarge)
                    if (account.isArchived) {
                        Text("Archived", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { onAction(AccountsAction.MoveUp(account)) }, enabled = !isFirst) {
                    Icon(LedgerIcons.MoveUp, contentDescription = "Move up")
                }
                IconButton(onClick = { onAction(AccountsAction.MoveDown(account)) }, enabled = !isLast) {
                    Icon(LedgerIcons.MoveDown, contentDescription = "Move down")
                }
                TextButton(onClick = { onAction(AccountsAction.RenameTapped(account)) }) { Text("Rename") }
                TextButton(onClick = { onAction(AccountsAction.ArchiveToggled(account)) }) {
                    Text(if (account.isArchived) "Unarchive" else "Archive")
                }
            }
        }
    }
}

@Composable
private fun AddAccountRow(state: AccountsUiState.Ready, onAction: (AccountsAction) -> Unit) {
    if (state.addingNew) {
        LedgerCard {
            LedgerTextField(
                value = state.newName,
                onValueChange = { onAction(AccountsAction.NewNameChanged(it)) },
                label = "New account name",
                isError = state.newNameError != null,
                supportingText = state.newNameError,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAction(AccountsAction.AddCancelled) }) { Text("Cancel") }
                LedgerPillButton(text = "Add", onClick = { onAction(AccountsAction.AddConfirmed) })
            }
        }
    } else {
        TextButton(onClick = { onAction(AccountsAction.AddTapped) }, modifier = Modifier.fillMaxWidth()) {
            Icon(LedgerIcons.Add, contentDescription = null)
            Text("Add account", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Preview
@Composable
private fun AccountsScreenPreview() {
    LedgerTheme {
        AccountsScreen(
            uiState = AccountsUiState.Ready(
                accounts = listOf(
                    Account("1", "client-1", "Domestic", 0, null),
                    Account("2", "client-2", "International", 1, null),
                    Account("3", "client-3", "Old Wallet", 2, archivedAt = java.time.Instant.now()),
                ),
                editingId = null,
                editingText = "",
                editingError = null,
                addingNew = false,
                newName = "",
                newNameError = null,
            ),
            onAction = {},
        )
    }
}
