package com.dumbopanda.ledger.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.PlaceholderInk
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.TrackOnDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.FirstRunField
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerHeroSheet
import com.dumbopanda.ledger.core.designsystem.component.LedgerMiniButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import com.dumbopanda.ledger.core.designsystem.component.LimeButton
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val fullDark = uiState == DeleteAccountUiState.Deleted ||
        uiState == DeleteAccountUiState.Deleting || uiState == DeleteAccountUiState.Reauthenticating
    SystemChromeEffect(if (fullDark) SystemChrome.Dark else SystemChrome.DarkOverCream)

    when (uiState) {
        DeleteAccountUiState.Deleted -> DeletedContent(onDone)
        DeleteAccountUiState.Deleting, DeleteAccountUiState.Reauthenticating -> ProgressContent(uiState)
        is DeleteAccountUiState.EmailReauth -> EmailReauthContent(uiState, onReauthCodeChanged, onVerifyReauthCode, onResendReauthCode, onCancel)
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

    // §7.17 step 2: "prominent, and the flow does not push past it quietly" -- an explicit
    // interrupt with two honest escape paths, same shape as SignOutGuardDialog (§7.16).
    if (showBackupNudge) {
        AlertDialog(
            onDismissRequest = { showBackupNudge = false },
            containerColor = White,
            title = { Text("Keep a copy first?", style = webText(20f, FontWeight.ExtraBold, TextDark)) },
            text = {
                Text(
                    "Once your account is deleted there's no undo. Save a file of your entries before you go.",
                    style = webText(15f, FontWeight.Normal, TextMuted, lineHeight = 22f),
                )
            },
            confirmButton = {
                LedgerPillButton(
                    text = "Save file",
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
                }) { Text("Delete without saving", style = webText(14f, FontWeight.Bold, Coral)) }
            },
        )
    }

    // §7.17 step 3: "type a word to confirm".
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
    LedgerHeroSheet(
        hero = {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)) {
                Eyebrow("Delete account")
                DisplayHeadline("Delete your account.", "Everything, for good.", size = 30f, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Text(
                    "It happens the moment you confirm, on every phone you use. There's no undo.",
                    style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f),
                )
            }
        },
        sheetTop = {
            if (uiState is DeleteAccountUiState.ReauthFailed) {
                LedgerBanner(text = uiState.message, tone = LedgerBannerTone.Error)
                Spacer(Modifier.height(16.dp))
            }
            if (uiState is DeleteAccountUiState.DeleteFailed) {
                LedgerBanner(text = "Couldn't delete your account: ${uiState.message}", tone = LedgerBannerTone.Error)
                Spacer(Modifier.height(16.dp))
            }
            Text("What gets deleted", style = webText(15f, FontWeight.Bold, TextDark))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoneItem("Every entry", Modifier.weight(1f))
                GoneItem("Your accounts", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoneItem("Your profile", Modifier.weight(1f))
                GoneItem("Your sign-in", Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .webBorder(HairlineLt, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Want to keep a copy?", style = webText(15f, FontWeight.Bold, TextDark))
                    Text(
                        lastBackupAt?.let { "File saved ${it.atZone(ZoneId.systemDefault()).format(SAVED_AT)}" } ?: "No file saved yet",
                        style = webText(13f, FontWeight.Normal, TextMuted),
                    )
                }
                LedgerMiniButton("Save file", onExport)
            }
        },
        sheetBottom = {
            DangerButton("Delete my account", onDeleteTapped)
            CancelLink(onCancel)
        },
    )
}

@Composable
private fun ProgressContent(uiState: DeleteAccountUiState) {
    Column(
        modifier = Modifier.fillMaxSize().background(CanvasDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Lime)
        Spacer(Modifier.height(20.dp))
        Text(
            if (uiState == DeleteAccountUiState.Reauthenticating) "Checking it's you…" else "Deleting your account…",
            style = webText(16f, FontWeight.SemiBold, White),
        )
    }
}

// C13: step 4 for an email-sign-in account -- a fresh code to the already-signed-in address
// (shown, never editable) stands in for Credential Manager's picker. Verifying it goes straight
// into delete(), same as a matched Google re-auth would.
@Composable
private fun EmailReauthContent(
    uiState: DeleteAccountUiState.EmailReauth,
    onCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
) {
    LedgerHeroSheet(
        hero = {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)) {
                Eyebrow("One last check")
                DisplayHeadline("Check your", "inbox.", size = 30f, breakBeforeAccent = false, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Text(
                    "We sent a 6-digit code to ${uiState.email}. Enter it to delete your account.",
                    style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f),
                )
            }
        },
        sheetTop = {
            if (uiState.sending) {
                Text("Sending your code…", style = webText(15f, FontWeight.SemiBold, TextMuted))
            } else {
                Text("Enter code", style = webText(13f, FontWeight.SemiBold, TextMuted))
                Spacer(Modifier.height(8.dp))
                // A one-time code, not a password: marked as one so Android offers to fill it
                // from the email instead of offering to save it in the password manager.
                FirstRunField(
                    value = uiState.code,
                    onValueChange = onCodeChanged,
                    placeholder = "6-digit code",
                    isError = uiState.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (uiState.code.length == 6) onVerify() }),
                    autofill = ContentType.SmsOtpCode,
                    description = "6-digit code",
                )
                if (uiState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error, style = webText(13f, FontWeight.SemiBold, Coral))
                }
                Spacer(Modifier.height(6.dp))
                val canResend = uiState.resendInS <= 0
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = canResend, role = Role.Button, onClick = onResend),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Didn't get it?", style = webText(14f, FontWeight.Normal, TextMuted))
                    Text(
                        if (canResend) "Resend code" else "Resend in %d:%02d".format(uiState.resendInS / 60, uiState.resendInS % 60),
                        style = webText(14f, FontWeight.Bold, if (canResend) Forest else PlaceholderInk),
                    )
                }
            }
        },
        sheetBottom = {
            DangerButton(
                if (uiState.verifying) "Checking your code…" else "Delete my account",
                onVerify,
                enabled = !uiState.sending && !uiState.verifying && uiState.code.length == 6,
            )
            CancelLink(onCancel)
        },
    )
}

@Composable
private fun DeletedContent(onDone: () -> Unit) {
    // Step 7: "Confirmation screen -> Onboarding."
    Column(
        modifier = Modifier.fillMaxSize().background(CanvasDark).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(72.dp).background(TrackOnDark, CircleShape), contentAlignment = Alignment.Center) {
            Icon(LedgerLineIcons.Check, contentDescription = null, tint = Lime, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(28.dp))
        DisplayHeadline("Your account is", "gone.", size = 30f, breakBeforeAccent = false, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Text(
            "Everything on it has been erased, and this phone's copy is cleared too.",
            style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        LimeButton(text = "Continue", onClick = onDone)
    }
}

@Composable
private fun TypeDeleteDialog(onDismiss: () -> Unit, onConfirmed: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text("Type DELETE to confirm", style = webText(20f, FontWeight.ExtraBold, TextDark)) },
        text = {
            Column {
                Text("This is the last step. There's no undo.", style = webText(15f, FontWeight.Normal, TextMuted, lineHeight = 22f))
                Spacer(Modifier.height(12.dp))
                FirstRunField(
                    value = typed,
                    onValueChange = { typed = it },
                    placeholder = "DELETE",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    description = "Type DELETE",
                )
            }
        },
        confirmButton = { DangerButton("Delete forever", onConfirmed, enabled = typed.trim() == "DELETE", fill = false) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = webText(14f, FontWeight.Bold, Forest)) } },
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(text.uppercase(), style = webText(12f, FontWeight.ExtraBold, Lime, letterSpacing = 1.4f))
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun GoneItem(text: String, modifier: Modifier) {
    Row(
        modifier.background(White, RoundedCornerShape(14.dp)).webBorder(HairlineLt, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(LedgerIcons.Close, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
        Text(text, style = webText(14f, FontWeight.SemiBold, TextDark))
    }
}

// Red, not the app's usual forest green: this button destroys things.
@Composable
private fun DangerButton(text: String, onClick: () -> Unit, enabled: Boolean = true, fill: Boolean = true) {
    Box(
        (if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (fill) 56.dp else 44.dp)
            .clip(CircleShape)
            .background(if (enabled) Coral else HairlineLt)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = webText(if (fill) 16f else 14f, FontWeight.Bold, if (enabled) White else TextMuted))
    }
}

@Composable
private fun CancelLink(onCancel: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Text("Keep my account", style = webText(14f, FontWeight.Bold, Forest))
    }
}

private val SAVED_AT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK)
