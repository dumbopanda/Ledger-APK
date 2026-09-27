package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.data.repository.EntrySyncMark
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.LedgerSheet
import com.dumbopanda.ledger.core.designsystem.component.LedgerSheetButton
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.component.SheetActions
import com.dumbopanda.ledger.core.designsystem.component.SheetButtonKind
import com.dumbopanda.ledger.core.designsystem.component.SheetLine
import com.dumbopanda.ledger.core.designsystem.component.SheetProps
import com.dumbopanda.ledger.core.designsystem.component.SheetTitle
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.domain.formatConfirmDate
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.formatMoneyFixed
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// index.html's stampText: "27 Sept, 00:15".
private val ADDED_FORMAT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK)

/** What the sheet says -- a pure function of the entry, so the wording is checked in one place. */
data class DetailContent(val title: String, val line: String, val rows: List<Pair<String, String?>>)

fun buildDetailContent(
    entry: Entry,
    sibling: Entry?,
    accounts: Map<String, Account>,
    syncMarks: Map<String, EntrySyncMark>,
    zone: ZoneId = ZoneId.systemDefault(),
): DetailContent {
    val amt = formatMoneyFixed(entry.amount.minor)
    val whenText = formatDisplayDate(entry.entryDate)
    val added = entry.createdAt.atZone(zone).format(ADDED_FORMAT)
    val marks = listOfNotNull(syncMarks[entry.clientUid], sibling?.let { syncMarks[it.clientUid] })
    val status = when {
        EntrySyncMark.Edited in marks -> "Changed on this phone. Not backed up yet."
        EntrySyncMark.OnDevice in marks -> "Saved on this phone. Not backed up yet."
        else -> "Backed up"
    }

    if (entry.transferGroup != null) {
        val sent = if (entry.direction == TxnDirection.Debit) entry else (sibling ?: entry)
        val received = if (entry.direction == TxnDirection.Debit) sibling else entry
        val from = accounts[sent.accountId]?.name.orEmpty()
        val to = received?.let { accounts[it.accountId]?.name }.orEmpty()
        return DetailContent(
            title = "Self transfer",
            line = "₹$amt moved from $from to $to on $whenText. Your own money, so it isn't counted as spending or income.",
            rows = listOf(
                "Amount" to "₹$amt moved",
                "From account" to from,
                "To account" to to,
                "Sent leg category" to sent.txnType,
                "Received leg category" to received?.txnType,
                "Transaction name" to entry.expenseName,
                "Note" to entry.moreInfo,
                "Date" to formatConfirmDate(entry.entryDate),
                "Reference" to entry.txnId.value,
                "Added" to added,
                "Status" to status,
            ),
        )
    }

    val account = accounts[entry.accountId]?.name.orEmpty()
    val out = entry.direction == TxnDirection.Debit
    return DetailContent(
        title = entry.expenseName.ifBlank { "Entry" },
        line = if (out) {
            "₹$amt went out of $account on $whenText for ${entry.expenseName}."
        } else {
            "₹$amt came into $account on $whenText for ${entry.expenseName}."
        },
        rows = listOf(
            "Amount" to "₹$amt ${if (out) "out" else "in"}",
            "Account" to account,
            "Category" to entry.txnType,
            "Transaction name" to entry.expenseName,
            "Note" to entry.moreInfo,
            "Date" to formatConfirmDate(entry.entryDate),
            "Will I get this back" to if (entry.txnId.isY) "Yes" else "No",
            "Reference" to entry.txnId.value,
            "Added" to added,
            "Status" to status,
        ),
    )
}

/**
 * `#detailSheet` -- ✕ top-right as the only "just close"; Delete | Edit 50/50; and, for a Y id
 * with both legs, the small Open | Close toggle top-right immediately left of the ✕ (the title
 * then reserves extra right padding so the name never runs under it).
 */
@Composable
fun EntryDetailSheet(
    state: EntryDetailState,
    accounts: Map<String, Account>,
    syncMarks: Map<String, EntrySyncMark>,
    onAction: (HistoryAction) -> Unit,
) {
    val hasLegs = state.owedToMeItem?.closeLegEntryId != null
    val content = buildDetailContent(state.entry, state.sibling, accounts, syncMarks)

    LedgerSheet(
        onDismissRequest = { onAction(HistoryAction.DetailDismissed) },
        topBar = {
            if (hasLegs) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 58.dp)
                        .clip(CircleShape)
                        .background(White)
                        .webBorder(HairlineLt, CircleShape)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LedgerPill("Open", state.showingOpenLeg, { onAction(HistoryAction.LegToggled(true)) }, style = PillStyles.Leg)
                    LedgerPill("Close", !state.showingOpenLeg, { onAction(HistoryAction.LegToggled(false)) }, style = PillStyles.Leg)
                }
            }
        },
    ) {
        SheetTitle(content.title, reserveEnd = if (hasLegs) 150.dp else 44.dp)
        SheetLine(content.line)
        SheetProps(content.rows)
        SheetActions(
            start = { m -> LedgerSheetButton("Delete", SheetButtonKind.Danger, { onAction(HistoryAction.DeleteTapped) }, m, enabled = !state.deleting) },
            end = { m -> LedgerSheetButton("Edit", SheetButtonKind.Save, { onAction(HistoryAction.EditTapped) }, m) },
        )
    }

    if (state.showDeleteConfirm) {
        // The web's own confirm(): "Delete this entry? This cannot be undone."
        AlertDialog(
            onDismissRequest = { onAction(HistoryAction.DeleteCancelled) },
            text = { Text("Delete this entry? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onAction(HistoryAction.DeleteConfirmed) }, enabled = !state.deleting) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { onAction(HistoryAction.DeleteCancelled) }) { Text("Cancel") } },
        )
    }
}
