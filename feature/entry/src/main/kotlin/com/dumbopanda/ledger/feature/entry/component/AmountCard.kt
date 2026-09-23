package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.model.TxnDirection

/**
 * §7.5.8 -- "the highest-risk field in the app." [amount] is always in CLEAN (ungrouped)
 * coordinates; the `en-IN` grouping the user sees is applied by [AmountVisualTransformation]
 * alone, so the caret math in [FormatAmountUseCase] only ever has to reason about one
 * coordinate space. [FormatAmountUseCase] has no dependencies of its own (unlike
 * [com.dumbopanda.ledger.core.domain.ParseDateInputUseCase]'s clock), so unlike `DateField`
 * this card owns its own instance rather than taking a pre-computed result from the caller.
 */
@Composable
fun AmountCard(
    amount: AmountFieldValue,
    onAmountChange: (AmountFieldValue) -> Unit,
    direction: TxnDirection,
    onDirectionChange: (TxnDirection) -> Unit,
    modifier: Modifier = Modifier,
    directionLocked: Boolean = false,
) {
    val useCase = remember { FormatAmountUseCase() }
    val visualTransformation = remember(useCase) { AmountVisualTransformation(useCase) }
    val lakhReadback = remember(amount.clean) { useCase.lakhReadback(amount.clean) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = TextFieldValue(amount.clean, TextRange(amount.caret)),
            onValueChange = { raw ->
                onAmountChange(useCase.applyKeystroke(amount, raw.text, raw.selection.end))
            },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = visualTransformation,
            textStyle = MaterialTheme.typography.headlineMedium,
            shape = LedgerShapes.extraSmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
        // "Not typed yet, not a value" -- no readback until there's a real amount.
        if (lakhReadback != null) {
            Text(
                text = lakhReadback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp),
            )
        }
        if (!directionLocked) {
            LedgerSegmentedButton(
                options = TxnDirection.entries,
                selected = direction,
                onSelect = onDirectionChange,
                label = { it.name },
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            // Self-transfer mode: both directions are true at once (§7.5.3), which a
            // single-choice segmented control can't represent -- shown as a locked label
            // instead of a toggle. The actual dual-lock behaviour is SelfTransferToggle's (B13).
            Text(
                text = "Debit & Credit (self transfer)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp),
            )
        }
    }
}

private class AmountVisualTransformation(
    private val useCase: FormatAmountUseCase,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val clean = text.text
        val display = useCase.toDisplay(clean)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = useCase.cleanToDisplayOffset(clean, offset)
            override fun transformedToOriginal(offset: Int): Int = useCase.displayToCleanOffset(clean, offset)
        }
        return TransformedText(AnnotatedString(display), offsetMapping)
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun AmountCardEmptyPreview() {
    LedgerTheme {
        AmountCard(
            amount = AmountFieldValue("", 0),
            onAmountChange = {},
            direction = TxnDirection.Debit,
            onDirectionChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun AmountCardLakhPreview() {
    LedgerTheme {
        AmountCard(
            amount = AmountFieldValue("1234567", 7),
            onAmountChange = {},
            direction = TxnDirection.Credit,
            onDirectionChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun AmountCardTransferLockedPreview() {
    LedgerTheme {
        AmountCard(
            amount = AmountFieldValue("50000", 5),
            onAmountChange = {},
            direction = TxnDirection.Debit,
            onDirectionChange = {},
            directionLocked = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
