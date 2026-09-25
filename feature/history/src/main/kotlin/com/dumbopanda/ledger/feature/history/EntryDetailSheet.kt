package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.component.LedgerBottomSheet
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DETAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.ENGLISH)

/**
 * §7.10, changed 24-25 Sep 2026: ✕ top-right is the only "just close this" affordance --
 * **no separate bottom Close button** -- and actions below the property list are Delete/Edit,
 * one row, 50/50. The Open/Close leg toggle sits top-right, immediately left of ✕ with a gap,
 * and only when [EntryDetailState.owedToMeItem] has both legs (`closeLegEntryId != null`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailSheet(state: EntryDetailState, onAction: (HistoryAction) -> Unit) {
    val entry = state.entry
    val hasToggle = state.owedToMeItem?.closeLegEntryId != null

    LedgerBottomSheet(onDismissRequest = { onAction(HistoryAction.DetailDismissed) }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.expenseName.ifBlank { "Self transfer" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(end = if (hasToggle) 12.dp else 8.dp),
            )
            if (hasToggle) {
                LedgerSegmentedButton(
                    options = listOf(true, false),
                    selected = state.showingOpenLeg,
                    onSelect = { onAction(HistoryAction.LegToggled(it)) },
                    label = { if (it) "Open" else "Close" },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            IconButton(onClick = { onAction(HistoryAction.DetailDismissed) }) {
                Icon(LedgerIcons.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        PropertyRow("Date", entry.entryDate.format(DETAIL_DATE_FORMAT))
        PropertyRow("Account", state.account?.name.orEmpty() + if (state.toAccount != null) " → ${state.toAccount.name}" else "")
        PropertyRow("Expense name", entry.expenseName)
        entry.moreInfo?.takeIf { it.isNotBlank() }?.let { PropertyRow("More info", it) }
        PropertyRow("Type", entry.txnType)
        PropertyRow("Amount", "₹" + FormatAmountUseCase().toDisplay(entry.amount.toCleanRupeeString()))
        PropertyRow("Transaction ID", entry.txnId.value)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { onAction(HistoryAction.DeleteTapped) }, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
            Button(onClick = { onAction(HistoryAction.EditTapped) }, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(HistoryAction.DeleteCancelled) },
            title = { Text("Delete this entry?") },
            text = { Text(state.deleteMessage) },
            confirmButton = {
                TextButton(onClick = { onAction(HistoryAction.DeleteConfirmed) }, enabled = !state.deleting) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(HistoryAction.DeleteCancelled) }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
