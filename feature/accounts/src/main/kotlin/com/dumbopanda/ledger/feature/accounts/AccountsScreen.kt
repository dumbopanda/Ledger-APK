package com.dumbopanda.ledger.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.LedgerInput
import com.dumbopanda.ledger.core.designsystem.component.LedgerMiniButton
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.model.Account

/**
 * Accounts (reached from Settings → Manage): rename, archive/unarchive, reorder, add -- the
 * APK's N accounts. Laid out as the web's account screen is: one panel, one line per account,
 * small pill buttons on the right.
 */
@Composable
fun AccountsRoute(header: @Composable () -> Unit, onBack: () -> Unit, viewModel: AccountsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsScreen(uiState, viewModel::onAction, header, onBack)
}

@Composable
internal fun AccountsScreen(
    uiState: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    header: @Composable () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        header()
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "‹ Settings",
                style = webText(14f, FontWeight.Bold, TextMuted),
                modifier = Modifier.webClickable(onClick = onBack).padding(vertical = 4.dp),
            )
            if (uiState is AccountsUiState.Ready) {
                AccountsPanel(uiState, onAction)
                AddPanel(uiState, onAction)
            }
        }
    }
}

@Composable
private fun AccountsPanel(state: AccountsUiState.Ready, onAction: (AccountsAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(LedgerShapes.medium).background(White).webBorder(HairlineStrong, LedgerShapes.medium),
    ) {
        state.accounts.forEachIndexed { index, account ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = HairlineLt)
            if (state.editingId == account.id) {
                EditRow(state.editingText, state.editingError, onAction)
            } else {
                AccountRow(account, isFirst = index == 0, isLast = index == state.accounts.lastIndex, onAction)
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, isFirst: Boolean, isLast: Boolean, onAction: (AccountsAction) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).alpha(if (account.isArchived) 0.55f else 1f)) {
            Text(account.name, style = webText(14f, FontWeight.SemiBold))
            if (account.isArchived) Text("Archived", style = webText(12f, FontWeight.Medium, TextMuted), modifier = Modifier.padding(top = 2.dp))
        }
        LedgerMiniButton("↑", { onAction(AccountsAction.MoveUp(account)) }, enabled = !isFirst)
        LedgerMiniButton("↓", { onAction(AccountsAction.MoveDown(account)) }, enabled = !isLast)
        LedgerMiniButton("Rename", { onAction(AccountsAction.RenameTapped(account)) })
        LedgerMiniButton(if (account.isArchived) "Restore" else "Archive", { onAction(AccountsAction.ArchiveToggled(account)) })
    }
}

@Composable
private fun EditRow(text: String, error: String?, onAction: (AccountsAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LedgerInput(text, { onAction(AccountsAction.RenameTextChanged(it)) }, placeholder = "Account name", invalid = error != null)
        if (error != null) Text(error, style = webText(12f, FontWeight.SemiBold, Coral))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LedgerMiniButton("Cancel", { onAction(AccountsAction.RenameCancelled) })
            LedgerMiniButton("Save", { onAction(AccountsAction.RenameConfirmed) }, primary = true)
        }
    }
}

@Composable
private fun AddPanel(state: AccountsUiState.Ready, onAction: (AccountsAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(LedgerShapes.medium).background(White).webBorder(HairlineStrong, LedgerShapes.medium),
    ) {
        if (state.addingNew) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LedgerInput(state.newName, { onAction(AccountsAction.NewNameChanged(it)) }, placeholder = "New account name", invalid = state.newNameError != null)
                state.newNameError?.let { Text(it, style = webText(12f, FontWeight.SemiBold, Coral)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LedgerMiniButton("Cancel", { onAction(AccountsAction.AddCancelled) })
                    LedgerMiniButton("Add", { onAction(AccountsAction.AddConfirmed) }, primary = true)
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Add an account", style = webText(14f, FontWeight.SemiBold))
                    Text("A bank, a card, cash, a wallet…", style = webText(12f, FontWeight.Medium, TextMuted), modifier = Modifier.padding(top = 2.dp))
                }
                LedgerMiniButton("Add", { onAction(AccountsAction.AddTapped) }, primary = true)
            }
        }
    }
}
