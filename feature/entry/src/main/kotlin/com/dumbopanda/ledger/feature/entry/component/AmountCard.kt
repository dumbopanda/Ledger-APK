package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.AmberInk
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.ChevronInk
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.model.TxnDirection

private val AmountStyle = webText(42f, FontWeight.SemiBold)

/**
 * `.amount-card`, exactly: white card (22/20 padding, 18px gaps, centred); the "Amount" label
 * with a "₹ INR" pill; a green ₹ beside a borderless 42/600 number that is only as wide as its
 * own digits (index.html's `updateAmountWidth`), so the pair sits centred; the amber lakh
 * note; then Credit | Debit. In self transfer both halves show on, dimmed and locked.
 *
 * [amount] is always CLEAN (ungrouped) text; the `en-IN` grouping is [AmountVisualTransformation]'s
 * alone, so the caret maths in [FormatAmountUseCase] only ever deals with one coordinate space.
 */
@Composable
fun AmountCard(
    amount: AmountFieldValue,
    onKeystroke: (rawText: String, rawCaret: Int) -> Unit,
    onBlur: () -> Unit,
    direction: TxnDirection,
    onDirectionChange: (TxnDirection) -> Unit,
    selfTransfer: Boolean,
    invalid: Boolean,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier,
) {
    val useCase = remember { FormatAmountUseCase() }
    val transformation = remember(useCase) { AmountVisualTransformation(useCase) }
    val lakh = remember(amount.clean) { useCase.lakhReadback(amount.clean) }
    var focused by remember { mutableStateOf(false) }

    // The web sizes the input to its text (or its placeholder while unfocused and empty),
    // plus 3px for the caret -- that's what keeps "₹" + the number centred as one unit.
    val measurer = rememberTextMeasurer()
    val display = useCase.toDisplay(amount.clean)
    val measured = when {
        display.isNotEmpty() -> display
        focused -> ""
        else -> "00.00"
    }
    val widthPx = if (measured.isEmpty()) 0 else measurer.measure(measured, AmountStyle).size.width
    val inputWidth = with(LocalDensity.current) { widthPx.toDp() } + 3.dp

    Column(
        modifier
            .fillMaxWidth()
            .background(White, LedgerShapes.medium)
            .webBorder(if (invalid) Coral else HairlineLt, LedgerShapes.medium)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Amount", style = webText(16f, FontWeight.Bold, letterSpacing = -0.2f))
            Text(
                "₹ INR",
                style = webText(12f, FontWeight.Bold),
                modifier = Modifier.clip(CircleShape).background(CanvasLight).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("₹", style = AmountStyle.copy(color = Forest), modifier = Modifier.alignByBaseline().padding(end = 4.dp))
            BasicTextField(
                value = TextFieldValue(amount.clean, TextRange(amount.caret)),
                onValueChange = { raw -> onKeystroke(raw.text, raw.selection.end) },
                singleLine = true,
                textStyle = AmountStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = transformation,
                cursorBrush = SolidColor(TextDark),
                modifier = Modifier
                    .alignByBaseline()
                    .width(inputWidth)
                    .then(inputModifier)
                    .onFocusChanged { state ->
                        if (focused && !state.isFocused) onBlur()
                        focused = state.isFocused
                    },
                decorationBox = { inner ->
                    Box {
                        if (amount.clean.isEmpty() && !focused) Text("00.00", style = AmountStyle.copy(color = ChevronInk), maxLines = 1)
                        inner()
                    }
                },
            )
        }
        if (lakh != null) {
            Spacer(Modifier.height(8.dp)) // .amt-note's margin-top: -10px against the 18px gap
            Text(
                lakh,
                style = webText(13f, FontWeight.Bold, AmberInk),
                modifier = Modifier.clip(CircleShape).background(Amber).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LedgerPill(
                text = "Credit",
                selected = selfTransfer || direction == TxnDirection.Credit,
                onClick = { onDirectionChange(TxnDirection.Credit) },
                style = PillStyles.Compact,
                locked = selfTransfer,
                modifier = Modifier.weight(1f),
            )
            LedgerPill(
                text = "Debit",
                selected = selfTransfer || direction == TxnDirection.Debit,
                onClick = { onDirectionChange(TxnDirection.Debit) },
                style = PillStyles.Compact,
                locked = selfTransfer,
                modifier = Modifier.weight(1f),
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
