package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerChip
import com.dumbopanda.ledger.core.domain.DateFieldState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DateQuickSelection { TODAY, YESTERDAY }

private val FULL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * §7.5.1. Free text, digits + `. - / space` only, no mask, no caret repositioning on a
 * rejected character. [dateState] is [com.dumbopanda.ledger.core.domain.ParseDateInputUseCase]'s
 * result for the field's current text, computed by the caller so this stays a plain
 * state-down/actions-up component (§6.7) rather than owning a clock itself.
 */
@Composable
fun DateField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    dateState: DateFieldState,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    // Which quick button (if any) currently describes the field's contents. Cleared the
    // moment the user types, per §7.5.1 -- purely ephemeral display state, not form data.
    var quickSelection by remember { mutableStateOf<DateQuickSelection?>(null) }
    var hasFocus by remember { mutableStateOf(false) }
    val latestDateState = rememberUpdatedState(dateState)

    val hint = computeDateFieldHint(dateState, today)
    val isError = hint is DateFieldHint.Error
    val hintText = when (hint) {
        is DateFieldHint.Placeholder -> hint.text
        is DateFieldHint.Warning -> hint.text
        is DateFieldHint.Error -> hint.text
        DateFieldHint.None -> " " // reserves the line's height so nothing jumps
    }
    val hintColor = when (hint) {
        is DateFieldHint.Warning -> Amber
        is DateFieldHint.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                quickSelection = null
                val filtered = filterDateFieldInput(raw.text, raw.selection.end)
                onValueChange(TextFieldValue(filtered.text, TextRange(filtered.caret)))
            },
            label = { Text("Date") },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = LedgerShapes.extraSmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .onFocusChanged { focusState ->
                    // Full rewrite on focus LOSS only, and only for a resolved date --
                    // unparseable text is left exactly as typed (§7.5.1).
                    if (hasFocus && !focusState.isFocused) {
                        val resolved = latestDateState.value
                        if (resolved is DateFieldState.Resolved) {
                            val rewritten = resolved.date.format(FULL_DATE_FORMAT)
                            onValueChange(TextFieldValue(rewritten, TextRange(rewritten.length)))
                        }
                    }
                    hasFocus = focusState.isFocused
                },
        )
        Text(
            text = hintText,
            style = MaterialTheme.typography.bodySmall,
            color = hintColor,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            LedgerChip(
                text = "Today",
                selected = quickSelection == DateQuickSelection.TODAY,
                onClick = {
                    quickSelection = DateQuickSelection.TODAY
                    val text = today.format(FULL_DATE_FORMAT)
                    onValueChange(TextFieldValue(text, TextRange(text.length)))
                },
            )
            LedgerChip(
                text = "Yesterday",
                selected = quickSelection == DateQuickSelection.YESTERDAY,
                onClick = {
                    quickSelection = DateQuickSelection.YESTERDAY
                    val text = today.minusDays(1).format(FULL_DATE_FORMAT)
                    onValueChange(TextFieldValue(text, TextRange(text.length)))
                },
            )
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun DateFieldEmptyPreview() {
    LedgerTheme {
        DateField(
            value = TextFieldValue(""),
            onValueChange = {},
            dateState = DateFieldState.Empty,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun DateFieldWarningPreview() {
    LedgerTheme {
        DateField(
            value = TextFieldValue("25.12"),
            onValueChange = {},
            dateState = DateFieldState.Resolved(LocalDate.of(2025, 12, 25), yearInferred = true, daysAgo = 273),
            today = LocalDate.of(2026, 9, 24),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun DateFieldErrorPreview() {
    LedgerTheme {
        DateField(
            value = TextFieldValue("31.4"),
            onValueChange = {},
            dateState = DateFieldState.Error("There is no 31 April"),
            modifier = Modifier.padding(16.dp),
        )
    }
}
