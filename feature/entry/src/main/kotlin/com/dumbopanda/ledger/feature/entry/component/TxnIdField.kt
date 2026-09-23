package com.dumbopanda.ledger.feature.entry.component

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
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.domain.GenerateTxnIdUseCase
import com.dumbopanda.ledger.core.model.TxnId

/**
 * §7.5.5. `txnId`'s kind (Y/N) drives everything else: changing it or tapping regenerate both
 * call [GenerateTxnIdUseCase] (owned locally -- it has no dependencies, same call as
 * `AmountCard`'s [com.dumbopanda.ledger.core.domain.FormatAmountUseCase]); picking an existing
 * open Y-ID from the dropdown does not generate anything, it just adopts that ID.
 */
@Composable
fun TxnIdField(
    txnId: TxnId,
    openYIds: List<TxnId>,
    onTxnIdChange: (TxnId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val useCase = remember { GenerateTxnIdUseCase() }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        LedgerSegmentedButton(
            options = listOf(false, true),
            selected = txnId.isY,
            onSelect = { isY -> onTxnIdChange(useCase.generate(isY)) },
            label = { if (it) "Y" else "N" },
        )
        Text(
            text = if (txnId.isY) "Y = returned later, new or old ID" else "N = one-way, new ID each time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp),
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            if (txnId.isY) {
                Text(
                    text = txnId.value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Box {
                    IconButton(onClick = { dropdownExpanded = true }) {
                        Icon(LedgerIcons.Regenerate, contentDescription = "Choose a Y-ID")
                    }
                    val freshId = remember(txnId, openYIds) { useCase.generate(isY = true) }
                    val options = remember(txnId, openYIds, freshId) {
                        buildYIdDropdownOptions(txnId, openYIds, freshId)
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.value) },
                                onClick = {
                                    onTxnIdChange(option)
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = txnId.value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
                IconButton(onClick = { onTxnIdChange(useCase.generate(isY = false)) }) {
                    Icon(LedgerIcons.Regenerate, contentDescription = "New ID")
                }
            }
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun TxnIdFieldNPreview() {
    LedgerTheme {
        TxnIdField(
            txnId = TxnId("Nabc123"),
            openYIds = emptyList(),
            onTxnIdChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun TxnIdFieldYPreview() {
    LedgerTheme {
        TxnIdField(
            txnId = TxnId("Yabc123"),
            openYIds = listOf(TxnId("Yopen01"), TxnId("Yopen02")),
            onTxnIdChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
