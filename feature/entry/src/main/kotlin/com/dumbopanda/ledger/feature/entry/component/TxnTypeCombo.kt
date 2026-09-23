package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val TXN_TYPE_OPTIONS = listOf(
    "Payment", "Refund", "Cashback", "Payment Split", "Salary", "Sent Transfer", "Received Transfer",
)
const val TXN_TYPE_CUSTOM = "Custom…"

/** Whether [type] needs the free-text field: anything not in the fixed list, including
 * whatever a user typed last time and saved -- that value must round-trip on reload (§7.5.6). */
fun isCustomTxnType(type: String): Boolean = type !in TXN_TYPE_OPTIONS

/**
 * §7.5.6. `selectedType` IS the stored value, whether it's one of [TXN_TYPE_OPTIONS] or an
 * arbitrary custom string -- [isCustomTxnType] derives whether to show the free-text field
 * from that single source of truth, rather than tracking "is custom" as separate state that
 * could drift out of sync with what's actually stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxnTypeCombo(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Transaction type",
) {
    var expanded by remember { mutableStateOf(false) }
    val isCustom = isCustomTxnType(selectedType)

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (isCustom) TXN_TYPE_CUSTOM else selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (TXN_TYPE_OPTIONS + TXN_TYPE_CUSTOM).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            // Custom starts empty only if we weren't already showing a custom
                            // value -- picking "Custom..." while a custom type is already
                            // stored must keep it, not blank it out from under the user.
                            onTypeChange(if (option == TXN_TYPE_CUSTOM) selectedType.takeIf { isCustom }.orEmpty() else option)
                        },
                    )
                }
            }
        }
        if (isCustom) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = onTypeChange,
                label = { Text("Custom type") },
                singleLine = true,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun TxnTypeComboFixedPreview() {
    com.dumbopanda.ledger.core.designsystem.LedgerTheme {
        TxnTypeCombo(
            selectedType = "Payment",
            onTypeChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun TxnTypeComboCustomPreview() {
    com.dumbopanda.ledger.core.designsystem.LedgerTheme {
        TxnTypeCombo(
            selectedType = "Diwali bonus",
            onTypeChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
