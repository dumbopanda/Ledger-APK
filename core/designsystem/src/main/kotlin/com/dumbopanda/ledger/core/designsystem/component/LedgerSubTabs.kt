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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.White

/**
 * `.sub-tabs` / `.sub-tab` -- a secondary switcher nested inside a screen (History's Recent /
 * Owed to me). A light, bordered track rather than [LedgerTopTabs]' dark one -- a second dark
 * pill directly under the dark top nav would read as two navs of equal rank -- but the same
 * dark-bg/lime-text language for whichever tab is active, and no per-item border.
 */
@Composable
fun <T> LedgerSubTabs(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = CircleShape,
                    color = if (active) CanvasDark else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 11.dp)) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Lime else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private enum class PreviewSubTab { RECENT, OWED }

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerSubTabsPreview() {
    LedgerTheme {
        LedgerSubTabs(
            options = PreviewSubTab.entries,
            selected = PreviewSubTab.RECENT,
            onSelect = {},
            label = { if (it == PreviewSubTab.RECENT) "Recent" else "Owed to me" },
            modifier = Modifier.padding(16.dp),
        )
    }
}
