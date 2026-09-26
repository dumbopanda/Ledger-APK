package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark

/**
 * `.top-tabs` / `.top-tab` -- the screen switcher (Add Payment | History | ...). A dark pill
 * track holding one pill per tab, each with a 2px lime border; the active tab fills lime with
 * dark text, inactive tabs stay transparent with a muted greenish-grey label. Distinct from
 * [LedgerSegmentedButton], which is Material's own default coloring and doesn't carry this
 * app's actual "primary navigation" look.
 */
@Composable
fun <T> LedgerTopTabs(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Surface(shape = CircleShape, color = CanvasDark, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = CircleShape,
                    color = if (active) Lime else Color.Transparent,
                    border = BorderStroke(2.dp, Lime),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 13.dp)) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (active) TextDark else MutedOnDark,
                        )
                    }
                }
            }
        }
    }
}

private enum class PreviewTab { ADD, HISTORY, ACCOUNTS, SETTINGS }

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerTopTabsPreview() {
    LedgerTheme {
        LedgerTopTabs(
            options = PreviewTab.entries,
            selected = PreviewTab.ADD,
            onSelect = {},
            label = { it.name },
            modifier = Modifier.padding(16.dp),
        )
    }
}
