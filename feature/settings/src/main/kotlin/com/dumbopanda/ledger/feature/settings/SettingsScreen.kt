package com.dumbopanda.ledger.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.data.sync.SyncMode
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.LedgerMiniButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.LedgerSwitch
import com.dumbopanda.ledger.core.designsystem.component.LocalLedgerToast
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.ui.openEmail
import com.dumbopanda.ledger.core.ui.openUrl
import java.time.LocalDate

private const val PRIVACY_POLICY_URL = "https://dumbopanda.github.io/Ledger-Pages/privacy/"
private const val TERMS_URL = "https://dumbopanda.github.io/Ledger-Pages/terms/"
private const val SUPPORT_EMAIL = "theledgerapp.support@gmail.com"

@Composable
fun SettingsRoute(
    header: @Composable () -> Unit,
    onAccountsRequested: () -> Unit,
    onSignedOut: () -> Unit,
    onDeleteAccountRequested: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalLedgerToast.current
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { onSignedOut() } }
    LaunchedEffect(viewModel) { viewModel.events.collect { toast.show(it) } }
    SettingsScreen(uiState, viewModel::onAction, header, onAccountsRequested, onDeleteAccountRequested)
}

/**
 * index.html's account screen -- "one line per thing, and everything that is only ever touched
 * when something has gone wrong folded away behind a summary" -- with the APK's own extras
 * (accounts, security, sign-in, about) laid out in the very same row-and-pill style.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    header: @Composable () -> Unit,
    onAccountsRequested: () -> Unit,
    onDeleteAccountRequested: () -> Unit,
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onAction(SettingsAction.ExportRequested(uri))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAction(SettingsAction.ImportRequested(uri))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        header()
        // `.account-screen { padding: 14px 20px 32px; gap: 14px }`
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Panel {
                AcctRow("Waiting to be sent", state.queueSummary) {
                    LedgerMiniButton(state.sendLabel, { onAction(SettingsAction.SendNowTapped) }, primary = true, enabled = state.sendEnabled)
                }
                RowDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("When to send", style = webText(14f, FontWeight.SemiBold), modifier = Modifier.padding(bottom = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(SyncMode.SMART to "Batched", SyncMode.INSTANT to "Instant", SyncMode.MANUAL to "Manual").forEach { (mode, label) ->
                            LedgerPill(label, state.syncMode == mode, { onAction(SettingsAction.SyncModeChanged(mode)) }, Modifier.weight(1f), PillStyles.SegChip)
                        }
                    }
                    Text(state.syncModeHint, style = webText(12f, FontWeight.Medium, TextMuted), modifier = Modifier.padding(top = 10.dp))
                }
            }

            Panel {
                AcctRow("Backup", state.backupStatus) {
                    LedgerMiniButton(
                        if (state.importing) "Restoring…" else "Restore",
                        { importLauncher.launch(arrayOf("application/json")) },
                        enabled = !state.importing,
                    )
                    LedgerMiniButton(
                        if (state.exporting) "Saving…" else "Save",
                        { exportLauncher.launch("ledger-backup-${LocalDate.now()}.json") },
                        primary = true,
                        enabled = !state.exporting,
                    )
                }
                RowDivider()
                AcctRow("Accounts", if (state.accountCount == 1) "1 account" else "${state.accountCount} accounts") {
                    LedgerMiniButton("Manage", onAccountsRequested)
                }
            }

            Panel {
                SwitchRow(
                    title = "Require fingerprint or screen lock to open",
                    desc = "Asked each time Ledger opens.",
                    checked = state.requireBiometricToOpen,
                    onChange = { onAction(SettingsAction.RequireBiometricToggled(it)) },
                )
                RowDivider()
                // The switch reads "hide", the stored preference is "show" -- so it's inverted
                // on the way in and on the way out.
                SwitchRow(
                    title = "Hide amounts in the app switcher",
                    desc = "Also blocks screenshots of Ledger.",
                    checked = !state.showAmountsInAppSwitcher,
                    onChange = { hide -> onAction(SettingsAction.ShowAmountsToggled(show = !hide)) },
                )
            }

            Panel {
                AcctRow("Signed in", state.signedInEmail ?: "—") {
                    LedgerMiniButton(if (state.signingOut) "Signing out…" else "Sign out", { onAction(SettingsAction.SignOutTapped) }, enabled = !state.signingOut)
                }
                RowDivider()
                AcctRow("Delete account", "Removes everything, on every device. Can't be undone.") {
                    LedgerMiniButton("Delete", onDeleteAccountRequested, danger = true)
                }
            }

            Fold("If something went wrong") {
                AcctRow(
                    "Couldn't be sent",
                    if (state.deadLetterCount == 0) {
                        "Nothing stuck — everything went through"
                    } else {
                        "${state.deadLetterCount} refused by the server, not retried"
                    },
                )
            }

            Panel {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                }
                AcctRow("Ledger", "Version ${version ?: "unknown"}")
                RowDivider()
                AcctRow("Privacy policy", null) { LedgerMiniButton("Open", { context.openUrl(PRIVACY_POLICY_URL) }) }
                RowDivider()
                AcctRow("Terms", null) { LedgerMiniButton("Open", { context.openUrl(TERMS_URL) }) }
                RowDivider()
                AcctRow("Support", SUPPORT_EMAIL) { LedgerMiniButton("Email", { context.openEmail(SUPPORT_EMAIL) }) }
            }
        }
    }

    if (state.showSignOutGuard) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.SignOutGuardDismissed) },
            title = {
                Text("${state.pendingOpCount} ${if (state.pendingOpCount == 1) "entry hasn't" else "entries haven't"} been sent yet.")
            },
            text = { Text("Signing out now keeps them on this device but they won't reach your account until you sign back in.") },
            confirmButton = { TextButton(onClick = { onAction(SettingsAction.SendPendingThenSignOutTapped) }) { Text("Send them first") } },
            dismissButton = { TextButton(onClick = { onAction(SettingsAction.SignOutConfirmed) }) { Text("Sign out anyway") } },
        )
    }
}

// `.panel` -- white, border-strong, 22px, rows separated by the lighter rule.
@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(LedgerShapes.medium)
            .background(White)
            .webBorder(HairlineStrong, LedgerShapes.medium),
        content = content,
    )
}

@Composable
private fun RowDivider() = HorizontalDivider(thickness = 1.dp, color = HairlineLt)

/** `.acct-row` -- title 14/600, summary 12/500 muted under it, pill buttons on the right. */
@Composable
private fun AcctRow(title: String, summary: String?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = webText(14f, FontWeight.SemiBold))
            if (summary != null) Text(summary, style = webText(12f, FontWeight.Medium, TextMuted), modifier = Modifier.padding(top = 2.dp))
        }
        actions()
    }
}

/** `.switch-row` inside a panel -- title 14/600, description 12/500 muted, the custom switch. */
@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = webText(14f, FontWeight.SemiBold))
            Text(desc, style = webText(12f, FontWeight.Medium, TextMuted))
        }
        LedgerSwitch(checked, onChange)
    }
}

/** `details.fold` -- a summary row with ⌄ / ⌃, its body separated by the lighter rule. */
@Composable
private fun Fold(summary: String, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Panel {
        Row(
            Modifier.fillMaxWidth().webClickable { open = !open }.padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(summary, style = webText(14f, FontWeight.Bold))
            Text(if (open) "⌃" else "⌄", style = webText(14f, FontWeight.ExtraBold, TextMuted))
        }
        if (open) {
            RowDivider()
            Column(Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}
