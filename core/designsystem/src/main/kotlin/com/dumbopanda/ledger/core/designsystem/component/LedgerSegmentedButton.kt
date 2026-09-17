package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerTheme

@Composable
fun <T> LedgerSegmentedButton(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.heightIn(min = 48.dp)) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label(option))
            }
        }
    }
}

private enum class Direction { Debit, Credit }

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerSegmentedButtonPreview() {
    LedgerTheme {
        LedgerSegmentedButton(
            options = Direction.entries,
            selected = Direction.Debit,
            onSelect = {},
            label = { it.name },
            modifier = Modifier.padding(16.dp),
        )
    }
}
