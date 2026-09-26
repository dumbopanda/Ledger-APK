package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerChip
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.GenerateTxnIdUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PICKER_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * `ui-spec.md` Add Payment: "Will I get this back?" -- replaces the old raw Y/N txn-id field.
 * No → the entry is written with a fresh `N` id, nothing further. Yes → New/Old: New mints a
 * fresh `Y` id, Old lists currently-open items by name/amount/date and adopting one closes
 * that pair. The id itself is never shown here (still exists underneath, shown in the confirm
 * sheet) -- hidden entirely by the caller while self transfer is on, since a transfer's `Y` id
 * is automatic (§7.5.3).
 */
@Composable
fun WillGetBackField(
    txnId: TxnId,
    openItems: List<OwedToMeItem>,
    onTxnIdChange: (TxnId) -> Unit,
    modifier: Modifier = Modifier,
    editFallback: TxnIdChoice.Existing? = null,
) {
    val generateTxnId = remember { GenerateTxnIdUseCase() }
    var oldPickerExpanded by remember { mutableStateOf(false) }

    val freshId = remember(txnId, openItems) { generateTxnId.generate(isY = true) }
    val choices = remember(txnId, openItems, freshId, editFallback) {
        buildTxnIdChoices(txnId, openItems, freshId, editFallback)
    }
    val current = choices.find { it.txnId == txnId } ?: choices.first()
    val isOld = current is TxnIdChoice.Existing

    Column(modifier = modifier) {
        Text(
            text = "Will I get this back?",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LedgerChip(
                text = "Yes",
                selected = txnId.isY,
                onClick = { if (!txnId.isY) onTxnIdChange(generateTxnId.generate(isY = true)) },
                modifier = Modifier.weight(1f),
            )
            LedgerChip(
                text = "No",
                selected = !txnId.isY,
                onClick = { if (txnId.isY) onTxnIdChange(generateTxnId.generate(isY = false)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (txnId.isY) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LedgerChip(
                    text = "New",
                    selected = !isOld,
                    onClick = { onTxnIdChange(generateTxnId.generate(isY = true)) },
                    modifier = Modifier.weight(1f),
                )
                LedgerChip(
                    text = "Old",
                    selected = isOld,
                    onClick = { if (openItems.isNotEmpty()) oldPickerExpanded = true },
                    modifier = Modifier.weight(1f),
                )
            }
            if (current is TxnIdChoice.Existing) {
                val existing = current
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "${existing.expenseName} · ${formatMoney(existing.amount)} · ${existing.date.format(PICKER_DATE_FORMAT)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    Box {
                        IconButton(onClick = { if (openItems.isNotEmpty()) oldPickerExpanded = true }) {
                            Icon(LedgerIcons.Regenerate, contentDescription = "Choose a different open item")
                        }
                        DropdownMenu(expanded = oldPickerExpanded, onDismissRequest = { oldPickerExpanded = false }) {
                            openItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.expenseName} · ${formatMoney(item.amount)} · ${item.openedDate.format(PICKER_DATE_FORMAT)}") },
                                    onClick = {
                                        onTxnIdChange(item.txnId)
                                        oldPickerExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMoney(amount: Money): String = "₹" + FormatAmountUseCase().toDisplay(amount.toCleanRupeeString())

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun WillGetBackFieldNoPreview() {
    LedgerTheme {
        WillGetBackField(
            txnId = TxnId("Nabc123"),
            openItems = emptyList(),
            onTxnIdChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun WillGetBackFieldOldPreview() {
    LedgerTheme {
        val item = OwedToMeItem(
            txnId = TxnId("Yopen01"),
            isOpen = true,
            expenseName = "Loan to Sam",
            amount = Money(50_00),
            accountId = "acc-1",
            openedDate = LocalDate.of(2026, 9, 10),
            closedDate = null,
            openLegEntryId = "entry-1",
            closeLegEntryId = null,
        )
        WillGetBackField(
            txnId = item.txnId,
            openItems = listOf(item),
            onTxnIdChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
