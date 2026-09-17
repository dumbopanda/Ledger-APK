package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme

// Presentational only — parsing/formatting minor units into this field's
// display string is FormatAmountUseCase's job (§6.6), not the design system's.
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.headlineMedium,
        shape = LedgerShapes.extraSmall,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    )
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun AmountFieldPreview() {
    LedgerTheme {
        AmountField(
            value = "4,500.00",
            onValueChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
