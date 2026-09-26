package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.LedgerTheme

// .chip.selected is dark-bg/lime-text -- the same "selected" language as every other
// toggle in the app (top-tab.active, kind-btn.on, sub-tab.active, toggle-pill[pressed]),
// not the primary/green used for actions elsewhere. Unselected keeps its 1px border
// even when selected state removes it visually (border color matches the fill then).
@Composable
fun LedgerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) CanvasDark else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Lime else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) CanvasDark else MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerChipPreview() {
    LedgerTheme {
        LedgerChip(text = "Cash", selected = true, onClick = {})
    }
}
