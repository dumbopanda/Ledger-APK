package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * The first-run flow's frame: a dark [hero] with a cream sheet rising under it. The sheet always
 * reaches the bottom edge -- [sheetTop] hangs from its top, [sheetBottom] sits on its floor -- and
 * the whole thing scrolls once it no longer fits (small phones, 2x font scale, the keyboard up),
 * so nothing is ever clipped.
 */
@Composable
fun LedgerHeroSheet(
    hero: @Composable ColumnScope.() -> Unit,
    sheetTop: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    heroAlignment: Alignment.Horizontal = Alignment.Start,
    sheetPadding: PaddingValues = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
    sheetBottom: @Composable ColumnScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CanvasDark)) {
        HeroThenFill(viewport = maxHeight, modifier = Modifier.verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = heroAlignment, content = hero)
            Column(
                Modifier.fillMaxWidth().background(CanvasLight, SheetShape).padding(sheetPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.fillMaxWidth(), content = sheetTop)
                Column(Modifier.fillMaxWidth().padding(top = 20.dp), content = sheetBottom)
            }
        }
    }
}

// Hero at its natural height; the sheet at least the rest of the viewport, more if it needs it.
@Composable
private fun HeroThenFill(viewport: Dp, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val hero = measurables[0].measure(loose)
        val sheetMin = (viewport.roundToPx() - hero.height).coerceAtLeast(0)
        val sheet = measurables[1].measure(loose.copy(minHeight = sheetMin))
        layout(constraints.maxWidth, hero.height + sheet.height) {
            hero.place(0, 0)
            sheet.place(0, hero.height)
        }
    }
}
