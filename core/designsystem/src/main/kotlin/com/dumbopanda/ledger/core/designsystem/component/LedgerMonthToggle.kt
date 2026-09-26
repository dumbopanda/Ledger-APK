package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * `.month-toggle` -- the one control History and Stats both draw, so they always describe the
 * same period. Next is disabled at the current month ("there is no such thing as a future
 * entry"); Prev stops at the oldest month allowed.
 */
@Composable
fun LedgerMonthToggle(
    label: String,
    sub: String,
    prevEnabled: Boolean,
    nextEnabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(White)
            .webBorder(HairlineStrong, CircleShape)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MonthArrow("‹", "Previous month", prevEnabled, onPrev)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = webText(15f, FontWeight.ExtraBold, letterSpacing = -0.2f))
            Text(sub, style = webText(11f, FontWeight.SemiBold, TextMuted))
        }
        MonthArrow("›", "Next month", nextEnabled, onNext)
    }
}

@Composable
private fun MonthArrow(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(if (enabled) CanvasLight else Color.Transparent)
            .webClickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = webText(18f, FontWeight.Bold))
    }
}
