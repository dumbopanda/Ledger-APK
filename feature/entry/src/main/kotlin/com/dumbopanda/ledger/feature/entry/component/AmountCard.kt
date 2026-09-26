package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerChip
import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.model.TxnDirection

private val AmountTextStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold)

/**
 * §7.5.8 -- "the highest-risk field in the app." [amount] is always in CLEAN (ungrouped)
 * coordinates; the `en-IN` grouping the user sees is applied by [AmountVisualTransformation]
 * alone, so the caret math in [FormatAmountUseCase] only ever has to reason about one
 * coordinate space.
 *
 * Visual structure matches the web front end's `.amount-card` exactly: a bordered white card
 * (label row with a "₹ INR" pill on the right, a centered green-₹-plus-large-number display
 * with no visible input box, an amber lakh-readback pill, then the Credit/Debit pair as the
 * same dark/lime pill language as every other selected control in the app) -- not a generic
 * Material outlined text field, which is what this used to be.
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

    Surface(
        shape = LedgerShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Amount", style = MaterialTheme.typography.titleMedium)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "₹ INR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "₹",
                    style = AmountTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Box {
                    if (amount.clean.isEmpty()) {
                        Text("00.00", style = AmountTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = TextFieldValue(amount.clean, TextRange(amount.caret)),
                        onValueChange = { raw ->
                            onAmountChange(useCase.applyKeystroke(amount, raw.text, raw.selection.end))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = visualTransformation,
                        textStyle = AmountTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    )
                }
            }

            if (lakhReadback != null) {
                Surface(shape = CircleShape, color = Amber) {
                    Text(
                        lakhReadback,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberDeepText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }

            if (!directionLocked) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgerChip(
                        text = "Credit",
                        selected = direction == TxnDirection.Credit,
                        onClick = { onDirectionChange(TxnDirection.Credit) },
                        modifier = Modifier.weight(1f),
                    )
                    LedgerChip(
                        text = "Debit",
                        selected = direction == TxnDirection.Debit,
                        onClick = { onDirectionChange(TxnDirection.Debit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // Self-transfer mode: both directions are true at once (§7.5.3), which a
                // single-choice control can't represent -- shown as a locked label instead.
                // The actual dual-lock behaviour is SelfTransferToggle's (B13).
                Text(
                    text = "Debit & Credit (self transfer)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// .amt-note's text color (#8A6D1F) on an Amber pill -- a warm brown-gold, not plain dark text.
private val AmberDeepText = androidx.compose.ui.graphics.Color(0xFF8A6D1F)

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
