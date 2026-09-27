package com.dumbopanda.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.InactiveOnDark
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect
import com.dumbopanda.ledger.core.designsystem.TrackOnDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.FirstRunChip
import com.dumbopanda.ledger.core.designsystem.component.FirstRunField
import com.dumbopanda.ledger.core.designsystem.component.IconBadge
import com.dumbopanda.ledger.core.designsystem.component.LimeButton
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText

private val SuggestedAccounts = listOf("Cash in hand", "Savings account", "Salary account", "Credit card", "UPI wallet", "Crypto")

// Dark enough that the cream Home behind reads as "not now", light enough that its "Add an
// account first" line above the sheet stays legible.
private val Scrim = Color(0x61121614)
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
fun AccountSetupRoute(onDone: () -> Unit, viewModel: AccountSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    AccountSetupOverlay(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onAddRow = viewModel::addRow,
        onRemoveRow = viewModel::removeRow,
        onUseSuggestion = viewModel::useSuggestion,
        onConfirm = viewModel::confirm,
        onDone = onDone,
    )
}

/**
 * The first-run "name your accounts" step, as a dark sheet rising over the real (dimmed, inert)
 * Home. It stops short of the top so Home's own "Add an account first" line stays readable above
 * it -- the reason this step exists, shown rather than told. Runs once, only for an account with
 * zero accounts ([AccountSetupViewModel]'s gate); not dismissable, since Home has nowhere to
 * record anything until it's done. Tapping anywhere that isn't a control just drops the keyboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountSetupOverlay(
    uiState: AccountSetupUiState,
    onNameChanged: (id: String, name: String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (id: String) -> Unit,
    onUseSuggestion: (String) -> Unit,
    onConfirm: () -> Unit,
    onDone: () -> Unit,
) {
    if (uiState is AccountSetupUiState.AlreadyHasAccounts || uiState is AccountSetupUiState.Done) {
        LaunchedEffect(uiState) { onDone() }
        return
    }
    if (uiState !is AccountSetupUiState.Ready) return // Checking: an instant local read

    SystemChromeEffect(SystemChrome(top = Scrim.compositeOver(CanvasLight), bottom = CanvasDark, lightIcons = true))
    val focusManager = LocalFocusManager.current
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    // With the keyboard up the sheet takes (almost) the whole height, so the field being typed
    // in and the button both stay on screen.
    val heightFraction by animateFloatAsState(if (WindowInsets.isImeVisible) 0.97f else 0.73f, tween(220), label = "sheetHeight")

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(shown, enter = fadeIn(tween(220))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            )
        }
        AnimatedVisibility(
            shown,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(360)) { it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .background(CanvasDark, SheetShape)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp),
                ) {
                    SheetContent(uiState, onNameChanged, onAddRow, onRemoveRow, onUseSuggestion, onConfirm)
                }
                Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
                    LimeButton(
                        text = if (uiState.saving) "Saving…" else "Start my Ledger",
                        onClick = {
                            focusManager.clearFocus()
                            onConfirm()
                        },
                        trailingIcon = if (uiState.saving) null else LedgerLineIcons.ArrowRight,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetContent(
    uiState: AccountSetupUiState.Ready,
    onNameChanged: (id: String, name: String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (id: String) -> Unit,
    onUseSuggestion: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val usedNames = uiState.rows.map { it.name.trim().lowercase() }.toSet()

    Text("LAST STEP", style = webText(12f, FontWeight.ExtraBold, Lime, letterSpacing = 1.4f))
    Spacer(Modifier.height(12.dp))
    DisplayHeadline("Where does your", "money live?", size = 30f, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(10.dp))
    Text(
        "An account is any place your money sits: a bank, your wallet, a card, a UPI app. " +
            "Every entry you log comes out of one or goes into one.",
        style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f),
    )
    Spacer(Modifier.height(18.dp))

    Fact(LedgerLineIcons.Plus, "One is enough to start.", "Add, rename or archive accounts anytime from your profile.")
    Fact(LedgerLineIcons.Return, "Two let you move money between them.", "Withdrew ₹2,000 at an ATM? Log it as a self transfer, Bank to Cash, not as spending.")
    Fact(LedgerLineIcons.Check, "Each keeps its own balance.", "Stats shows what's left in every account, not one blurred total.")
    Spacer(Modifier.height(22.dp))

    Text("Name your accounts", style = webText(15f, FontWeight.Bold, White))
    Spacer(Modifier.height(10.dp))
    uiState.rows.forEachIndexed { index, row ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FirstRunField(
                    value = row.name,
                    onValueChange = { onNameChanged(row.id, it) },
                    placeholder = if (index == 0) "e.g. SBI Savings" else "Another account",
                    isError = row.error != null,
                    onDark = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onConfirm()
                    }),
                    description = "Account ${index + 1} name",
                )
            }
            if (uiState.rows.size > 1) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onRemoveRow(row.id) }) {
                    Icon(LedgerIcons.Close, contentDescription = "Remove this account", tint = InactiveOnDark)
                }
            }
        }
        if (row.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(row.error, style = webText(13f, FontWeight.SemiBold, Coral))
        }
        Spacer(Modifier.height(10.dp))
    }
    Text("Or tap to add", style = webText(13f, FontWeight.SemiBold, MutedOnDark))
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestedAccounts.forEach { name ->
            FirstRunChip(
                text = name,
                selected = name.lowercase() in usedNames,
                onClick = {
                    focusManager.clearFocus()
                    onUseSuggestion(name)
                },
                onDark = true,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onAddRow)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(LedgerLineIcons.Plus, contentDescription = null, tint = Lime, modifier = Modifier.size(20.dp))
        Text("Add your own", style = webText(14f, FontWeight.Bold, Lime))
    }
}

@Composable
private fun Fact(icon: ImageVector, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, TrackOnDark, Lime, size = 34.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = webText(14f, FontWeight.Bold, White))
            Text(detail, style = webText(13f, FontWeight.Normal, MutedOnDark, lineHeight = 18f))
        }
    }
}

@Preview(heightDp = 840)
@Composable
private fun AccountSetupOverlayPreview() {
    LedgerTheme {
        Box(Modifier.fillMaxSize().background(CanvasLight)) {
            AccountSetupOverlay(
                uiState = AccountSetupUiState.Ready(rows = listOf(AccountSetupRow(name = "Cash in hand")), saving = false),
                onNameChanged = { _, _ -> },
                onAddRow = {},
                onRemoveRow = {},
                onUseSuggestion = {},
                onConfirm = {},
                onDone = {},
            )
        }
    }
}
