package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.AmberInk
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.HintGrey
import com.dumbopanda.ledger.core.designsystem.component.FieldLabel
import com.dumbopanda.ledger.core.designsystem.component.LedgerInput
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.domain.DateFieldState
import com.dumbopanda.ledger.feature.entry.QuickDate
import java.time.LocalDate

/**
 * `.date-field` -- the text input (16/600, "e.g. 5.9") with Today and Yesterday on the SAME
 * row, stretched to the input's height (`.date-row { align-items: stretch }`); the hint sits
 * under the row's right edge, absolutely positioned so it never pushes the next field down.
 */
@Composable
fun DateField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onBlur: () -> Unit,
    dateState: DateFieldState,
    quickDate: QuickDate?,
    onQuickPick: (QuickDate) -> Unit,
    invalid: Boolean,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    var hadFocus by remember { mutableStateOf(false) }
    val hint = computeDateFieldHint(dateState, today)
    val (hintText, hintStyle) = when (hint) {
        is DateFieldHint.Placeholder -> hint.text to webText(11f, FontWeight.Medium, HintGrey, lineHeight = 12f)
        is DateFieldHint.Warning -> hint.text to webText(11f, FontWeight.SemiBold, AmberInk, lineHeight = 12f)
        is DateFieldHint.Error -> hint.text to webText(11f, FontWeight.SemiBold, Coral, lineHeight = 12f)
        DateFieldHint.None -> "" to webText(11f, FontWeight.Medium, HintGrey, lineHeight = 12f)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Date")
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LedgerInput(
                    value = value,
                    onValueChange = { raw ->
                        val filtered = filterDateFieldInput(raw.text, raw.selection.end)
                        onValueChange(TextFieldValue(filtered.text, TextRange(filtered.caret)))
                    },
                    placeholder = "e.g. 5.9",
                    keyboardType = KeyboardType.Decimal,
                    invalid = invalid,
                    textStyle = webText(16f, FontWeight.SemiBold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                    onFocusChange = { focused ->
                        if (hadFocus && !focused) onBlur()
                        hadFocus = focused
                    },
                    modifier = Modifier.weight(1f).then(inputModifier),
                )
                LedgerPill(
                    text = "Today",
                    selected = quickDate == QuickDate.TODAY,
                    onClick = { onQuickPick(QuickDate.TODAY) },
                    style = PillStyles.QuickDate,
                    modifier = Modifier.fillMaxHeight(),
                )
                LedgerPill(
                    text = "Yesterday",
                    selected = quickDate == QuickDate.YESTERDAY,
                    onClick = { onQuickPick(QuickDate.YESTERDAY) },
                    style = PillStyles.QuickDate,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            if (hintText.isNotEmpty()) {
                // top: 100% + 4px margin; the line is 12px tall, so 16px below the row's bottom.
                Text(
                    hintText,
                    style = hintStyle,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 16.dp)
                        .fillMaxWidth(0.72f),
                )
            }
        }
    }
}
