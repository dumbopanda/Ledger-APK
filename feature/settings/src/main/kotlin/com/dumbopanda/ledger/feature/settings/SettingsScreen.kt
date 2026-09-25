package com.dumbopanda.ledger.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.data.sync.SyncMode
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerCard
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton

/**
 * §7.13. **Not built here** (present as informational/disabled rows, not silently omitted):
 * Backup (Save/Restore) is C8's job -- no export/import exists yet. Delete account is C11's --
 * 7 steps and a public web page, neither of which exist. "Require biometric to open" persists
 * its toggle but enforces nothing yet -- the gate belongs in §7.1's Boot routing, C9's job,
 * which doesn't exist to gate. "Hide amounts in the app switcher" is the one security toggle
 * that's actually wired end to end -- MainActivity applies `FLAG_SECURE` from it.
 */
@Composable
fun SettingsRoute(onAccountsRequested: () -> Unit = {}, onSignedOut: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { onSignedOut() } }
    SettingsScreen(uiState = uiState, onAction = viewModel::onAction, onAccountsRequested = onAccountsRequested)
}

@Composable
internal fun SettingsScreen(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit, onAccountsRequested: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item { SyncSection(uiState, onAction) }
            item { AccountsSection(onAccountsRequested) }
            item { BackupSection() }
            item { SecuritySection(uiState, onAction) }
            item { AboutSection() }
            item { AccountSection(uiState, onAction) }
        }
    }

    if (uiState.showSignOutGuard) {
        SignOutGuardDialog(uiState.pendingOpCount, onAction)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun SyncSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SectionTitle("Sync")
    LedgerCard {
        LedgerSegmentedButton(
            options = listOf(SyncMode.SMART, SyncMode.INSTANT, SyncMode.MANUAL),
            selected = uiState.syncMode,
            onSelect = { onAction(SettingsAction.SyncModeChanged(it)) },
            label = {
                when (it) {
                    SyncMode.SMART -> "Together"
                    SyncMode.INSTANT -> "Instantly"
                    SyncMode.MANUAL -> "Manual"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = syncModeDescription(uiState.syncMode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(queueSummary(uiState.pendingOpCount, uiState.deadLetterCount), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onAction(SettingsAction.SendNowTapped) }) { Text("Send now") }
        }
    }
}

private fun syncModeDescription(mode: SyncMode): String = when (mode) {
    SyncMode.SMART -> "Wait and send them together"
    SyncMode.INSTANT -> "As soon as I add them"
    SyncMode.MANUAL -> "Only when I tap Send now"
}

private fun queueSummary(pending: Int, deadLettered: Int): String = buildString {
    append(if (pending == 0) "Nothing waiting to send" else "$pending waiting to send")
    if (deadLettered > 0) append(" · $deadLettered couldn't be sent")
}

@Composable
private fun AccountsSection(onAccountsRequested: () -> Unit) {
    SectionTitle("Accounts")
    LedgerCard(onClick = onAccountsRequested) {
        Text("Manage accounts", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BackupSection() {
    SectionTitle("Backup")
    LedgerCard {
        Text(
            "Export and import aren't built yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}, enabled = false) { Text("Save a backup file") }
            OutlinedButton(onClick = {}, enabled = false) { Text("Restore from a file") }
        }
    }
}

@Composable
private fun SecuritySection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SectionTitle("Security")
    LedgerCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Require biometric to open", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Saved, not enforced yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = uiState.requireBiometricToOpen, onCheckedChange = { onAction(SettingsAction.RequireBiometricToggled(it)) })
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hide amounts in the app switcher", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = uiState.showAmountsInAppSwitcher,
                onCheckedChange = { onAction(SettingsAction.ShowAmountsToggled(it)) },
            )
        }
    }
}

@Composable
private fun AboutSection() {
    SectionTitle("About")
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    LedgerCard {
        Text("Version ${versionName ?: "unknown"}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Privacy policy · Terms · Open-source licences · Support",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Links go live with the Play listing (C13)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SectionTitle("Account")
    LedgerCard {
        Text(uiState.signedInEmail ?: "Signed in", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAction(SettingsAction.SignOutTapped) }, enabled = !uiState.signingOut) {
                Text("Sign out")
            }
        }
        TextButton(onClick = { onAction(SettingsAction.DeleteAccountTapped) }, enabled = false, modifier = Modifier.padding(top = 4.dp)) {
            Text("Delete account", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SignOutGuardDialog(pendingCount: Int, onAction: (SettingsAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(SettingsAction.SignOutGuardDismissed) },
        title = { Text("$pendingCount ${if (pendingCount == 1) "entry hasn't" else "entries haven't"} been sent yet.") },
        text = {
            Text(
                "Signing out now keeps them on this device but they won't reach your account until you sign back in.",
            )
        },
        confirmButton = {
            LedgerPillButton(text = "Send them first", onClick = { onAction(SettingsAction.SendPendingThenSignOutTapped) })
        },
        dismissButton = {
            TextButton(onClick = { onAction(SettingsAction.SignOutConfirmed) }) { Text("Sign out anyway") }
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SettingsScreenPreview() {
    LedgerTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                syncMode = SyncMode.SMART,
                pendingOpCount = 3,
                deadLetterCount = 0,
                showAmountsInAppSwitcher = false,
                requireBiometricToOpen = false,
                signedInEmail = "user@example.com",
                showSignOutGuard = false,
                signingOut = false,
            ),
            onAction = {},
            onAccountsRequested = {},
        )
    }
}
