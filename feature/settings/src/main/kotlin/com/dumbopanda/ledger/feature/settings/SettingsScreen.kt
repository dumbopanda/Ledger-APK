package com.dumbopanda.ledger.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerCard
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import java.time.LocalDate

/**
 * §7.13. **Not built here** (present as informational/disabled rows, not silently omitted):
 * "Require biometric to open" persists its toggle -- the actual gate lives in the app module's
 * `BiometricGate` (C9), wrapping Restoring/Home, not here. "Hide amounts in the app switcher"
 * is the one security toggle MainActivity itself enforces via `FLAG_SECURE`. Export/import
 * (§7.14) are real, via SAF launchers owned by this Composable (the ViewModel only ever sees a
 * [android.net.Uri], never the picker itself). Delete account (C11) is real too, but lives on
 * its own screen (`DeleteAccountRoute`) rather than a Settings dialog, per §7.17's own 7-step
 * shape -- this row only navigates there. The nightly nudge banner shows here rather than
 * app-wide, since there's no shared shell yet to host a global banner (C9's job) -- see this
 * Composable's own body for where it renders.
 */
@Composable
fun SettingsRoute(
    onAccountsRequested: () -> Unit = {},
    onSignedOut: () -> Unit = {},
    onDeleteAccountRequested: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { onSignedOut() } }
    LaunchedEffect(viewModel) { viewModel.events.collect { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } }
    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onAccountsRequested = onAccountsRequested,
        onDeleteAccountRequested = onDeleteAccountRequested,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onAccountsRequested: () -> Unit,
    onDeleteAccountRequested: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (uiState.showNightlyNudge) {
                item { NightlyNudgeBanner(onAction) }
            }
            item { SyncSection(uiState, onAction) }
            item { AccountsSection(onAccountsRequested) }
            item { BackupSection(uiState, onAction) }
            item { SecuritySection(uiState, onAction) }
            item { AboutSection() }
            item { AccountSection(uiState, onAction, onDeleteAccountRequested) }
        }
    }

    if (uiState.showSignOutGuard) {
        SignOutGuardDialog(uiState.pendingOpCount, onAction)
    }
}

@Composable
private fun NightlyNudgeBanner(onAction: (SettingsAction) -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        LedgerBanner(text = "No backup since last night. Save one below?", tone = LedgerBannerTone.Warning)
        TextButton(onClick = { onAction(SettingsAction.NudgeSnoozed) }) { Text("Remind me later") }
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
private fun BackupSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SectionTitle("Backup")
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onAction(SettingsAction.ExportRequested(uri))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAction(SettingsAction.ImportRequested(uri))
    }
    LedgerCard {
        Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { exportLauncher.launch("ledger-backup-${LocalDate.now()}.json") },
                enabled = !uiState.exporting,
            ) { Text(if (uiState.exporting) "Saving…" else "Save a backup file") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                enabled = !uiState.importing,
            ) { Text(if (uiState.importing) "Importing…" else "Restore from a file") }
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
private fun AccountSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit, onDeleteAccountRequested: () -> Unit) {
    SectionTitle("Account")
    LedgerCard {
        Text(uiState.signedInEmail ?: "Signed in", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAction(SettingsAction.SignOutTapped) }, enabled = !uiState.signingOut) {
                Text("Sign out")
            }
        }
        TextButton(onClick = onDeleteAccountRequested, modifier = Modifier.padding(top = 4.dp)) {
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
                exporting = false,
                importing = false,
                showNightlyNudge = true,
            ),
            onAction = {},
            onAccountsRequested = {},
        )
    }
}
