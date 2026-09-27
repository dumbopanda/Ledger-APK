package com.dumbopanda.ledger.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerTextField
import java.time.LocalDate

@Composable
fun DeleteAccountRoute(
    onAccountDeleted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Step 7's "Confirmation screen" IS DeleteAccountScreen's own Deleted content below --
    // onAccountDeleted only fires the navigation once that content has already rendered and
    // the person taps Continue, not automatically as soon as uiState becomes Deleted.
    DeleteAccountScreen(
        uiState = uiState,
        lastBackupAt = lastBackupAt,
        hasExportedThisSession = viewModel.hasExportedThisSession,
        onExport = { uri -> viewModel.export(uri) { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } },
        onConfirmDeletion = { viewModel.reauthenticateAndDelete(context) },
        onReauthCodeChanged = viewModel::onReauthCodeChanged,
        onVerifyReauthCode = viewModel::verifyReauthCodeAndDelete,
        onResendReauthCode = viewModel::resendReauthCode,
        onDone = onAccountDeleted,
        onCancel = onCancel,
    )
}

@Composable
internal fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    lastBackupAt: java.time.Instant?,
    hasExportedThisSession: Boolean,
    onExport: (android.net.Uri) -> Unit,
    onConfirmDeletion: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onReauthCodeChanged: (String) -> Unit = {},
    onVerifyReauthCode: () -> Unit = {},
    onResendReauthCode: () -> Unit = {},
) {
    var showBackupNudge by remember { mutableStateOf(false) }
    var showTypeDeleteDialog by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onExport(uri)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            DeleteAccountUiState.Deleted -> DeletedContent(onDone)
            DeleteAccountUiState.Deleting, DeleteAccountUiState.Reauthenticating -> ProgressContent(uiState)
            is DeleteAccountUiState.EmailReauth -> EmailReauthContent(uiState, onReauthCodeChanged, onVerifyReauthCode, onResendReauthCode)
            else -> ConfirmingContent(
                uiState = uiState,
                lastBackupAt = lastBackupAt,
                onExport = { exportLauncher.launch("ledger-backup-${LocalDate.now()}.json") },
                onDeleteTapped = {
                    if (hasExportedThisSession) showTypeDeleteDialog = true else showBackupNudge = true
                },
                onCancel = onCancel,
            )
        }
    }

    // §7.17 step 2: "prominent, and the flow does not push past it quietly" -- an explicit
    // interrupt with two honest escape paths, same shape as SignOutGuardDialog (§7.16).
    if (showBackupNudge) {
        AlertDialog(
            onDismissRequest = { showBackupNudge = false },
            title = { Text("You haven't saved a backup") },
            text = { Text("Once your account is deleted there's no undo. Save a file first if you want to keep a copy.") },
            confirmButton = {
                LedgerPillButton(
                    text = "Save a backup",
                    onClick = {
                        showBackupNudge = false
                        exportLauncher.launch("ledger-backup-${LocalDate.now()}.json")
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupNudge = false
                    showTypeDeleteDialog = true
                }) { Text("Continue without backing up") }
            },
        )
    }

    // §7.17 step 3: the first "type a word to confirm" pattern in this codebase -- an
    // AlertDialog + OutlinedTextField, matching the AlertDialog shape every other destructive
    // confirmation here already uses (EntryDetailSheet's delete dialog, SignOutGuardDialog).
    if (showTypeDeleteDialog) {
        TypeDeleteDialog(
            onDismiss = { showTypeDeleteDialog = false },
            onConfirmed = {
                showTypeDeleteDialog = false
                onConfirmDeletion()
            },
        )
    }
}

@Composable
private fun ConfirmingContent(
    uiState: DeleteAccountUiState,
    lastBackupAt: java.time.Instant?,
    onExport: () -> Unit,
    onDeleteTapped: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Delete your account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        // Step 1: "exactly what is deleted... and that it is immediate and irreversible."
        Text(
            // C13: was "your Google sign-in link" -- now true for either sign-in method.
            "This deletes your entries, your accounts, your profile, and your sign-in. " +
                "It happens immediately and cannot be undone.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))

        if (uiState is DeleteAccountUiState.ReauthFailed) {
            LedgerBanner(text = uiState.message, tone = LedgerBannerTone.Error)
            Spacer(Modifier.height(16.dp))
        }
        if (uiState is DeleteAccountUiState.DeleteFailed) {
            LedgerBanner(text = "Couldn't delete your account: ${uiState.message}", tone = LedgerBannerTone.Error)
            Spacer(Modifier.height(16.dp))
        }

        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Save a backup file")
        }
        Text(
            text = if (lastBackupAt != null) "Last backup: $lastBackupAt" else "No backup saved yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(32.dp))
        LedgerPillButton(text = "Delete my account", onClick = onDeleteTapped, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ProgressContent(uiState: DeleteAccountUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            if (uiState == DeleteAccountUiState.Reauthenticating) "Verifying it's you..." else "Deleting your account...",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// C13: step 4 for an email-sign-in account -- a fresh code to the already-signed-in address
// (shown, never editable: unlike feature:auth's sign-in flow, there's nothing to type here)
// stands in for Credential Manager's picker. Verifying it goes straight into delete(), same as
// a matched Google re-auth would.
@Composable
private fun EmailReauthContent(
    uiState: DeleteAccountUiState.EmailReauth,
    onCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verify it's you", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter the 6-digit code sent to ${uiState.email} to confirm deletion.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (uiState.error != null) {
            LedgerBanner(text = uiState.error, tone = LedgerBannerTone.Error)
        }
        if (uiState.sending) {
            CircularProgressIndicator()
        } else {
            LedgerTextField(
                value = uiState.code,
                onValueChange = onCodeChanged,
                label = "6-digit code",
                keyboardType = KeyboardType.NumberPassword,
                isError = uiState.error != null,
            )
            LedgerPillButton(
                text = "Verify and delete",
                onClick = onVerify,
                enabled = !uiState.verifying && uiState.code.length == 6,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onResend, enabled = uiState.resendInS <= 0) {
                Text(if (uiState.resendInS > 0) "Resend code in ${uiState.resendInS}s" else "Resend code")
            }
        }
    }
}

@Composable
private fun DeletedContent(onDone: () -> Unit) {
    // Step 7: "Confirmation screen -> Onboarding."
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Your account has been deleted", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Everything on your account is gone. This device's local copy has been cleared too.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        LedgerPillButton(text = "Continue", onClick = onDone)
    }
}

@Composable
private fun TypeDeleteDialog(onDismiss: () -> Unit, onConfirmed: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type DELETE to confirm") },
        text = {
            Column {
                Text("This is the last step. It cannot be undone.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = typed, onValueChange = { typed = it }, singleLine = true, label = { Text("DELETE") })
            }
        },
        confirmButton = {
            LedgerPillButton(text = "Delete forever", onClick = onConfirmed, enabled = typed == "DELETE")
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
