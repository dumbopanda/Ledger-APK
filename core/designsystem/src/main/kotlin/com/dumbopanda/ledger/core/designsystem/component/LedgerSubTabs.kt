package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.CountOnDark
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText

/** What sits after a sub-tab's label: `.count` (neutral) or `.badge` (amber, "needs you"). */
sealed interface SubTabMark {
    data class Count(val value: Int) : SubTabMark
    data class Badge(val value: Int) : SubTabMark
}

/**
 * `.sub-tabs` / `.sub-tab` -- History's Recent / Owed to me. A white, bordered track (5px
 * padding and gap) rather than a second dark pill, since two dark pills stacked read as two
 * navs of equal rank; the active tab is dark with lime text, 14/700, 11px vertical padding.
 */
@Composable
fun <T> LedgerSubTabs(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    mark: (T) -> SubTabMark? = { null },
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(White)
            .webBorder(HairlineLt, CircleShape)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Row(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (active) TextDark else Color.Transparent)
                    .webClickable(role = Role.Tab) { onSelect(option) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label(option), style = webText(14f, FontWeight.Bold, if (active) Lime else TextMuted), maxLines = 1)
                mark(option)?.let { SubTabMarkPill(it, active) }
            }
        }
    }
}

@Composable
private fun SubTabMarkPill(mark: SubTabMark, active: Boolean) {
    val (fill, ink, value) = when (mark) {
        is SubTabMark.Badge -> Triple(Amber, TextDark, mark.value)
        is SubTabMark.Count -> if (active) Triple(CountOnDark, MutedOnDark, mark.value) else Triple(CanvasLight, TextMuted, mark.value)
    }
    Box(
        Modifier
            .height(20.dp)
            .defaultMinSize(minWidth = 22.dp)
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(value.toString(), style = webText(12f, FontWeight.ExtraBold, ink))
    }
}
