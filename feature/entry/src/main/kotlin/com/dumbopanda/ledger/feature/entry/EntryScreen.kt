package com.dumbopanda.ledger.feature.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.AmberDeepInk
import com.dumbopanda.ledger.core.designsystem.AmberInk
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.component.GroupInset
import com.dumbopanda.ledger.core.designsystem.component.LedgerComboField
import com.dumbopanda.ledger.core.designsystem.component.LedgerCta
import com.dumbopanda.ledger.core.designsystem.component.LedgerField
import com.dumbopanda.ledger.core.designsystem.component.LedgerGroup
import com.dumbopanda.ledger.core.designsystem.component.LedgerInput
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.LedgerSheet
import com.dumbopanda.ledger.core.designsystem.component.LedgerSheetButton
import com.dumbopanda.ledger.core.designsystem.component.LocalLedgerToast
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.component.SheetActions
import com.dumbopanda.ledger.core.designsystem.component.SheetButtonKind
import com.dumbopanda.ledger.core.designsystem.component.SheetLine
import com.dumbopanda.ledger.core.designsystem.component.SheetNote
import com.dumbopanda.ledger.core.designsystem.component.SheetProps
import com.dumbopanda.ledger.core.designsystem.component.SheetTitle
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.domain.DraftField
import com.dumbopanda.ledger.feature.entry.component.AccountChipRows
import com.dumbopanda.ledger.feature.entry.component.AmountCard
import com.dumbopanda.ledger.feature.entry.component.CategoryCombo
import com.dumbopanda.ledger.feature.entry.component.DateField
import com.dumbopanda.ledger.feature.entry.component.LegCategorySelect
import com.dumbopanda.ledger.feature.entry.component.WillGetBackGroup
import com.dumbopanda.ledger.feature.entry.component.isCustomTxnType

/**
 * index.html's Add Payment form: [header] (the app's title bar, tabs and any notice bars) sits
 * at the top of the SAME scroll, exactly as the web page scrolls them away with the form.
 */
@Composable
fun EntryRoute(
    header: @Composable () -> Unit,
    onEditSaved: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalLedgerToast.current
    val focus = remember { EntryFocus() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EntryEvent.Toast -> toast.show(event.message, event.durationMs)
                EntryEvent.EditSaved -> onEditSaved()
                is EntryEvent.FocusField -> focus.request(event.field, uiState)
            }
        }
    }
    EntryScreen(uiState = uiState, onAction = viewModel::onAction, header = header, focus = focus)
}

@Composable
internal fun EntryScreen(
    uiState: EntryUiState,
    onAction: (EntryAction) -> Unit,
    header: @Composable () -> Unit,
    focus: EntryFocus = remember { EntryFocus() },
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        header()
        when (uiState) {
            EntryUiState.Loading -> Unit
            EntryUiState.NoAccounts -> Text(
                "Add an account first — there's nowhere to record this until one exists.",
                style = webText(14f, FontWeight.Medium, TextMuted),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
            )
            is EntryUiState.Ready -> EntryForm(uiState, onAction, focus)
        }
    }
    if (uiState is EntryUiState.Ready && uiState.confirm != null) {
        ConfirmSheet(uiState.confirm, uiState.saving, onAction)
    }
}

// `form { padding: 14px 20px 20px; gap: 14px }` -- four cards, not nine loose fields.
@Composable
private fun EntryForm(state: EntryUiState.Ready, onAction: (EntryAction) -> Unit, focus: EntryFocus) {
    val draft = state.draft
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.isEditing) EditBanner(state.editBannerSub.orEmpty()) { onAction(EntryAction.CancelEdit) }

        // 1 · AMOUNT
        AmountCard(
            amount = draft.amount,
            onKeystroke = { raw, caret -> onAction(EntryAction.AmountKeystroke(raw, caret)) },
            onBlur = { onAction(EntryAction.AmountBlurred) },
            direction = draft.direction,
            onDirectionChange = { onAction(EntryAction.DirectionChanged(it)) },
            selfTransfer = draft.selfTransfer,
            invalid = state.invalid == DraftField.AMOUNT,
            inputModifier = Modifier.focusRequester(focus.amount),
        )

        // 2 · WHICH ACCOUNT -- Self transfer sits top-right of the card, not in the flow.
        LedgerGroup(
            title = "Which account",
            overlay = {
                LedgerPill(
                    text = "Self transfer",
                    selected = draft.selfTransfer,
                    onClick = { onAction(EntryAction.SelfTransferToggled(!draft.selfTransfer)) },
                    style = PillStyles.Toggle,
                    leading = "⇄",
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
                )
            },
        ) {
            Column(Modifier.padding(start = GroupInset)) {
                LedgerField(if (draft.selfTransfer) "From account" else "Account") {
                    AccountChipRows(state.accounts, draft.accountId, { onAction(EntryAction.AccountChanged(it)) })
                }
                // `.to-slot` -- max-height/opacity transition, 240ms.
                AnimatedVisibility(
                    visible = draft.selfTransfer,
                    enter = expandVertically(tween(240)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(240)) + fadeOut(tween(160)),
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        LedgerField("To account") {
                            AccountChipRows(state.accounts, draft.toAccountId, { onAction(EntryAction.ToAccountChanged(it)) })
                        }
                    }
                }
            }
        }

        // 3 · WHAT IT WAS -- name, note, date, category.
        LedgerGroup(title = "What it was") {
            LedgerField("Transaction name", Modifier.padding(start = GroupInset)) {
                NameCombo(state, onAction, focus.name)
            }
            LedgerField("Note", Modifier.padding(start = GroupInset), labelSuffix = "(optional)") {
                LedgerInput(
                    value = draft.moreInfo.orEmpty(),
                    onValueChange = { onAction(EntryAction.MoreInfoChanged(it)) },
                    placeholder = "Notes",
                )
            }
            DateField(
                value = state.dateText,
                onValueChange = { onAction(EntryAction.DateTextChanged(it)) },
                onBlur = { onAction(EntryAction.DateBlurred) },
                dateState = state.dateFieldState,
                quickDate = state.quickDate,
                onQuickPick = { onAction(EntryAction.DateQuickPicked(it)) },
                invalid = state.invalid == DraftField.DATE,
                modifier = Modifier.padding(start = GroupInset),
                inputModifier = Modifier.focusRequester(focus.date),
            )
            if (draft.selfTransfer) {
                SplitCategories(draft.sentLegType, draft.receivedLegType, onAction)
            } else {
                LedgerField("Category", Modifier.padding(start = GroupInset)) {
                    CategoryCombo(draft.txnType, { onAction(EntryAction.TxnTypeChanged(it)) })
                }
            }
        }

        // 4 · WILL I GET THIS BACK -- not for a self transfer, whose Y id is automatic.
        if (!draft.selfTransfer) {
            WillGetBackGroup(
                txnId = draft.txnId,
                backOld = state.backOld,
                openItems = state.openItems,
                onBack = { onAction(EntryAction.BackChanged(it)) },
                onBackKind = { onAction(EntryAction.BackKindChanged(it)) },
                onOpenItem = { onAction(EntryAction.OpenItemChosen(it)) },
            )
        }

        LedgerCta(text = state.ctaText, onClick = { onAction(EntryAction.SaveTapped) }, enabled = !state.saving)
        if (state.caption != null) {
            // .cta-caption { margin-top: -8px } against the form's 14px gap.
            Text(
                state.caption,
                style = webText(13f, FontWeight.Medium, TextMuted),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offsetUp(8),
            )
        }
    }
}

private sealed interface NameOption {
    data object Placeholder : NameOption
    data class Known(val name: String) : NameOption
    data object AddNew : NameOption
}

// "The name is picked, not typed. Free text is what splits 'Rapido' from 'rapido' and makes
// spend-by-name meaningless, so the list is the field and adding to it is a deliberate act."
@Composable
private fun NameCombo(state: EntryUiState.Ready, onAction: (EntryAction) -> Unit, customFocus: FocusRequester) {
    val name = state.draft.expenseName
    val options = buildList {
        add(NameOption.Placeholder)
        state.knownNames.forEach { add(NameOption.Known(it)) }
        add(NameOption.AddNew)
    }
    LedgerComboField(
        options = options,
        selected = if (name.isBlank()) NameOption.Placeholder else NameOption.Known(name),
        onSelect = { option ->
            when (option) {
                NameOption.Placeholder -> onAction(EntryAction.ExpenseNameChosen(""))
                is NameOption.Known -> onAction(EntryAction.ExpenseNameChosen(option.name))
                NameOption.AddNew -> onAction(EntryAction.AddNewNameChosen)
            }
        },
        optionLabel = { option ->
            when (option) {
                NameOption.Placeholder -> "Choose a name…"
                is NameOption.Known -> option.name
                NameOption.AddNew -> "Add a new name…"
            }
        },
        customMode = state.nameCustomMode,
        customValue = name,
        onCustomValueChange = { onAction(EntryAction.ExpenseNameTyped(it)) },
        customPlaceholder = "Name this one",
        invalid = state.invalid == DraftField.NAME,
        optionItalic = { it == NameOption.AddNew },
        isCustomOption = { it == NameOption.AddNew },
        customInputModifier = Modifier.focusRequester(customFocus),
    )
}

// Self transfer: the category splits into its two legs, side by side (`.row-2`), with any
// "Custom…" leg's own text field in a second row 18px below.
@Composable
private fun SplitCategories(sent: String, received: String, onAction: (EntryAction) -> Unit) {
    Column(Modifier.padding(start = GroupInset), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LedgerField("Sent leg category", Modifier.weight(1f)) {
                LegCategorySelect(sent, { onAction(EntryAction.SentLegTypeChanged(it)) })
            }
            LedgerField("Received leg category", Modifier.weight(1f)) {
                LegCategorySelect(received, { onAction(EntryAction.ReceivedLegTypeChanged(it)) })
            }
        }
        val sentCustom = isCustomTxnType(sent)
        val receivedCustom = isCustomTxnType(received)
        if (sentCustom || receivedCustom) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sentCustom) {
                    LedgerField("Custom (sent)", Modifier.weight(1f)) {
                        LedgerInput(sent, { onAction(EntryAction.SentLegTypeChanged(it)) }, placeholder = "Type your own")
                    }
                }
                if (receivedCustom) {
                    LedgerField("Custom (received)", Modifier.weight(1f)) {
                        LedgerInput(received, { onAction(EntryAction.ReceivedLegTypeChanged(it)) }, placeholder = "Type your own")
                    }
                }
                // A grid cell the hidden one leaves empty keeps the visible one at half width.
                if (!(sentCustom && receivedCustom)) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// `.edit-banner` -- amber, inset a further 20px (its own margin inside the form's padding).
@Composable
private fun EditBanner(sub: String, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 2.dp)
            .clip(LedgerShapes.medium)
            .background(Amber)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Editing an entry", style = webText(14f, FontWeight.ExtraBold, AmberDeepInk))
            Text(sub, style = webText(12f, FontWeight.SemiBold, AmberInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(TextDark)
                .webClickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text("Cancel", style = webText(13f, FontWeight.Bold, Lime))
        }
    }
}

@Composable
private fun ConfirmSheet(content: ConfirmSheetContent, saving: Boolean, onAction: (EntryAction) -> Unit) {
    LedgerSheet(onDismissRequest = { onAction(EntryAction.ConfirmDismissed) }) {
        SheetTitle(content.title)
        SheetLine(content.line)
        content.note?.let { SheetNote(it) }
        SheetProps(content.rows)
        SheetActions(
            start = { m -> LedgerSheetButton(content.backLabel, SheetButtonKind.Outline, { onAction(EntryAction.ConfirmDismissed) }, m) },
            end = { m -> LedgerSheetButton(content.confirmLabel, SheetButtonKind.Save, { onAction(EntryAction.ConfirmConfirmed) }, m, enabled = !saving) },
        )
    }
}

/** The three fields a blocked save can point at, so the caret lands where the problem is. */
internal class EntryFocus {
    val amount = FocusRequester()
    val date = FocusRequester()
    val name = FocusRequester()

    fun request(field: DraftField, state: EntryUiState) {
        if (state !is EntryUiState.Ready) return
        when (field) {
            DraftField.AMOUNT -> amount.requestFocus()
            DraftField.DATE -> date.requestFocus()
            // A picked name is a dropdown, not a text box -- its red border is the pointer.
            // Only the typed "Name this one" input can take the caret.
            DraftField.NAME -> if (state.nameCustomMode) name.requestFocus()
        }
    }
}

// A negative top margin: pull this element up by [dp] into the gap above it.
private fun Modifier.offsetUp(dp: Int): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val shift = dp.dp.roundToPx()
        layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
    },
)
