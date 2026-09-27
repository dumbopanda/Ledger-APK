package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CountOnDark
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * The first-run flow's display headline: [lead] in white, [accent] in lime -- the one place the
 * brand's lime carries words, so it always lands on the promise ("Every IOU, too.").
 * [breakBeforeAccent] puts the accent on its own line.
 */
@Composable
fun DisplayHeadline(
    lead: String,
    accent: String,
    modifier: Modifier = Modifier,
    size: Float = 34f,
    breakBeforeAccent: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    leadColor: Color = White,
) {
    Text(
        buildAnnotatedString {
            append(lead)
            append(if (breakBeforeAccent) "\n" else " ")
            withStyle(SpanStyle(color = Lime)) { append(accent) }
        },
        style = webText(size, FontWeight.ExtraBold, leadColor, letterSpacing = -0.8f, lineHeight = size + 6f),
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** Full-width lime pill on the dark hero, 56 tall -- onboarding's forward action. */
@Composable
fun LimeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailingIcon: ImageVector? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(Lime)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = webText(16f, FontWeight.ExtraBold, CanvasDark))
        if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = CanvasDark, modifier = Modifier.size(20.dp))
    }
}

/** A round tinted icon well: the row icon in value lists and the data rows. Decorative. */
@Composable
fun IconBadge(icon: ImageVector, fill: Color, ink: Color, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(fill, CircleShape).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(size * 0.52f))
    }
}

/** Onboarding's progress: the current pane is a 24-wide pill, the rest dots. */
@Composable
fun PageDots(count: Int, current: Int, onDark: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 24.dp else 6.dp, label = "dot")
            val color by animateColorAsState(
                when {
                    active && onDark -> Lime
                    active -> Forest
                    onDark -> CountOnDark
                    else -> HairlineStrong
                },
                label = "dotColor",
            )
            Box(Modifier.width(width).height(6.dp).background(color, CircleShape))
        }
    }
}
