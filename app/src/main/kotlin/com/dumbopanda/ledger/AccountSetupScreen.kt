package com.dumbopanda.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerTextField
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons

@Composable
fun AccountSetupRoute(onDone: () -> Unit, viewModel: AccountSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    AccountSetupScreen(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onAddRow = viewModel::addRow,
        onRemoveRow = viewModel::removeRow,
        onConfirm = viewModel::confirm,
        onDone = onDone,
    )
}

/**
 * Runs once, between Restoring and Home, only for a brand-new user (zero accounts restored
 * from the server). Replaces the old silent seed of Domestic/International/Cash -- accounts
 * we personally use, not a new client's -- with names they actually typed.
 */
@Composable
internal fun AccountSetupScreen(
    uiState: AccountSetupUiState,
    onNameChanged: (id: String, name: String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (id: String) -> Unit,
    onConfirm: () -> Unit,
    onDone: () -> Unit,
) {
    if (uiState is AccountSetupUiState.AlreadyHasAccounts || uiState is AccountSetupUiState.Done) {
        LaunchedEffect(uiState) { onDone() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            AccountSetupUiState.Checking, AccountSetupUiState.AlreadyHasAccounts, AccountSetupUiState.Done -> {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            is AccountSetupUiState.Ready -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            ) {
                Text("What accounts do you use?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Everyone tracks money differently -- name whatever accounts you actually " +
                        "use (a bank, cash, a card, a wallet app...). You can add, rename or " +
                        "archive these later from Accounts.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                uiState.rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        LedgerTextField(
                            value = row.name,
                            onValueChange = { onNameChanged(row.id, it) },
                            label = "Account name",
                            isError = row.error != null,
                            supportingText = row.error,
                            modifier = Modifier.weight(1f),
                        )
                        if (uiState.rows.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { onRemoveRow(row.id) }) {
                                Icon(LedgerIcons.Close, contentDescription = "Remove this account")
                            }
                        }
                    }
                }

                TextButton(onClick = onAddRow) { Text("+ Add another account") }
                Spacer(Modifier.height(16.dp))
                LedgerPillButton(
                    text = "Continue",
                    onClick = onConfirm,
                    enabled = !uiState.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun AccountSetupScreenPreview() {
    LedgerTheme {
        AccountSetupScreen(
            uiState = AccountSetupUiState.Ready(
                rows = listOf(
                    AccountSetupRow(name = "Bank"),
                    AccountSetupRow(name = ""),
                ),
                saving = false,
            ),
            onNameChanged = { _, _ -> },
            onAddRow = {},
            onRemoveRow = {},
            onConfirm = {},
            onDone = {},
        )
    }
}
