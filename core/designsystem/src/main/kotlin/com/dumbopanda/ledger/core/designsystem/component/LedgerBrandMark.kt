package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.webText

/** The launcher icon, drawn: a lime disc with a dark rupee sign. Decorative -- the wordmark or
 *  headline next to it carries the name. */
@Composable
fun LedgerBrandMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(Lime, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text("₹", style = webText(size.value * 0.53f, FontWeight.ExtraBold, CanvasDark, lineHeight = size.value * 0.6f))
    }
}
