package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.webText

// index.html's own inline SVG, path for path: a circle head (12,8 r4) and the shoulders arc.
private val PersonOutline: ImageVector = ImageVector.Builder(
    name = "PersonOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(16f, 8f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -8f, dy1 = 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 8f, dy1 = 0f)
        close()
    }
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(4f, 21f)
        curveToRelative(0f, -4f, 3.5f, -7f, 8f, -7f)
        reflectiveCurveToRelative(8f, 3f, 8f, 7f)
    }
}.build()

/**
 * `.gear-btn` + `.gear-inner` + `.pending-count` -- a 52px dark circle, 6px in from which a
 * 2px lime ring holds the person icon; the ring fills lime (and the icon goes dark) while the
 * Settings screen is open. The lime badge counts entries still waiting to be sent.
 */
@Composable
fun LedgerProfileButton(active: Boolean, pendingCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CanvasDark)
                .webClickable(onClick = onClick)
                .padding(6.dp),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (active) Lime else CanvasDark)
                    .border(2.dp, Lime, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PersonOutline,
                    contentDescription = "Settings and sync",
                    tint = if (active) TextDark else MutedOnDark,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (pendingCount > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .height(20.dp)
                    .defaultMinSize(minWidth = 20.dp)
                    .clip(CircleShape)
                    .background(CanvasLight)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Lime)
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(pendingCount.toString(), style = webText(11f, FontWeight.ExtraBold, TextDark))
            }
        }
    }
}
