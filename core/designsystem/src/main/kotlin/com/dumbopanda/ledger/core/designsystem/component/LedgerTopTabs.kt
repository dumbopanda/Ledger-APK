package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * `.top-tabs` / `.top-tab` -- the screen switcher. A dark pill track (6px padding, 6px gap)
 * holding one pill per tab, each with a 2px lime border and 13px vertical padding; the active
 * tab fills lime with dark 14/700 text, the rest are transparent with #9EAD9C text. [selected]
 * is null while the Settings screen is open -- the web then highlights no tab at all.
 */
@Composable
fun <T> LedgerTopTabs(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(CanvasDark)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (active) Lime else Color.Transparent)
                    .webBorder(Lime, CircleShape, 2.dp)
                    .webClickable(role = Role.Tab) { onSelect(option) }
                    .padding(vertical = 13.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = webText(14f, FontWeight.Bold, if (active) TextDark else MutedOnDark),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

private enum class PreviewTab { ADD, HISTORY, STATS }

@Preview(fontScale = 1.0f)
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
