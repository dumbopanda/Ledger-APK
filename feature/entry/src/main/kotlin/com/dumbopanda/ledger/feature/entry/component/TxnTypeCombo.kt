package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dumbopanda.ledger.core.designsystem.component.LedgerComboField
import com.dumbopanda.ledger.core.designsystem.component.LedgerSelect

// index.html's <option> list, in its order.
val TXN_TYPE_OPTIONS = listOf(
    "Payment", "Refund", "Cashback", "Payment Split", "Salary", "Sent Transfer", "Received Transfer",
)
const val TXN_TYPE_CUSTOM = "Custom…"

/** Whether [type] needs the free-text field: anything not in the fixed list, including
 * whatever a user typed last time and saved -- that value must round-trip on reload (§7.5.6). */
fun isCustomTxnType(type: String): Boolean = type !in TXN_TYPE_OPTIONS

private val CATEGORY_CHOICES = TXN_TYPE_OPTIONS + TXN_TYPE_CUSTOM

/**
 * "Category" (`#txnType` + `#txnTypeCustomInput`): a dropdown whose "Custom…" turns the field
 * into a text box ("Enter a category of your own"), arrow still on the right to switch back.
 * [value] IS the stored type, so whether the text box shows is derived, never tracked apart.
 */
@Composable
fun CategoryCombo(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val custom = isCustomTxnType(value)
    LedgerComboField(
        options = CATEGORY_CHOICES,
        selected = if (custom) TXN_TYPE_CUSTOM else value,
        onSelect = { option ->
            // Re-picking Custom… while already custom keeps what was typed (the web's <select>
            // fires no change event for picking the option it already shows).
            if (option == TXN_TYPE_CUSTOM) {
                if (!custom) onChange("")
            } else {
                onChange(option)
            }
        },
        optionLabel = { it },
        customMode = custom,
        customValue = value,
        onCustomValueChange = onChange,
        customPlaceholder = "Enter a category of your own",
        hideCustomPlaceholderWhileFocused = true,
        optionItalic = { it == TXN_TYPE_CUSTOM },
        modifier = modifier,
    )
}

/**
 * Self transfer's "Sent leg category" / "Received leg category" (`#txnTypeFrom`/`#txnTypeTo`):
 * plain dropdowns -- their "Custom…" opens a separate text field in the row below instead.
 */
@Composable
fun LegCategorySelect(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val custom = isCustomTxnType(value)
    LedgerSelect(
        options = CATEGORY_CHOICES,
        selected = if (custom) TXN_TYPE_CUSTOM else value,
        onSelect = { option ->
            if (option == TXN_TYPE_CUSTOM) {
                if (!custom) onChange("")
            } else {
                onChange(option)
            }
        },
        optionLabel = { it },
        optionItalic = { it == TXN_TYPE_CUSTOM },
        modifier = modifier,
    )
}
