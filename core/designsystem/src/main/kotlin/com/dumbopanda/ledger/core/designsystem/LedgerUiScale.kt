package com.dumbopanda.ledger.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * index.html's `--ui-scale: 0.9` -- "everything is drawn at full size and then rendered down,
 * the way browser zoom works". Scaling density (and so every dp AND sp) by the same factor is
 * the Compose equivalent, which is what lets every size in this app be written as the web's
 * own CSS px value and still come out at the web's rendered size. The user's own font-size
 * preference still applies on top, via the untouched fontScale.
 */
const val LEDGER_UI_SCALE = 0.9f

@Composable
fun LedgerUiScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density * LEDGER_UI_SCALE, density.fontScale),
        content = content,
    )
}

private val CssLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** A text style in the web's own terms: size in CSS px, weight, colour, letter-spacing in px. */
fun webText(
    size: Float,
    weight: FontWeight,
    color: Color = TextDark,
    letterSpacing: Float = 0f,
    lineHeight: Float? = null,
    italic: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = LedgerFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
    // CSS splits a line-height's extra space evenly above and below every line, the first and
    // last included; Compose's default trims it off those two, which squeezes multi-line text.
    lineHeightStyle = if (lineHeight != null) CssLineHeight else null,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
)
