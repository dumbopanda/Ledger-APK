package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.AmberInk
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.LEDGER_UI_SCALE
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerUiScale
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * `.modal-overlay` + `.modal` -- index.html's bottom sheet: cream, 24px top corners, 24/20
 * padding, 14px between children, a 32px ✕ pinned top 18 / right 18 as the only "just close"
 * affordance, and [topBar] for anything that sits beside the ✕ (the Open/Close leg toggle).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerSheet(
    onDismissRequest: () -> Unit,
    topBar: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // The sheet is its own window, drawn at the device's real density (see below), so its
        // one size given outside the content is pre-scaled by hand.
        shape = RoundedCornerShape(topStart = (24 * LEDGER_UI_SCALE).dp, topEnd = (24 * LEDGER_UI_SCALE).dp),
        containerColor = CanvasLight,
        scrimColor = Color(0x80121614),
        dragHandle = null,
    ) {
        // A sheet's window resets LocalDensity to the device's own, dropping the web's 0.9
        // zoom that the rest of the app is drawn at -- put it back for everything inside.
        LedgerUiScale { SheetBody(onDismissRequest, topBar, content) }
    }
}

@Composable
private fun SheetBody(
    onDismissRequest: () -> Unit,
    topBar: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        topBar()
        SheetClose(onDismissRequest, Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 18.dp))
    }
}

@Composable
private fun SheetClose(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(White)
            .webBorder(HairlineLt, CircleShape)
            .webClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", style = webText(14f, FontWeight.Bold, TextMuted))
    }
}

/** `.sheet-title` -- 19/800, with right padding reserved for the ✕ (and the leg toggle). */
@Composable
fun SheetTitle(text: String, reserveEnd: Dp = 44.dp) {
    Text(text, style = webText(19f, FontWeight.ExtraBold), modifier = Modifier.padding(end = reserveEnd))
}

/** `.sheet-line` -- 14/500 at 1.55 line height. */
@Composable
fun SheetLine(text: String) {
    Text(text, style = webText(14f, FontWeight.Medium, lineHeight = 14f * 1.55f))
}

/** `.sheet-note` -- the amber pill ("That is 5 lakh — check the zeros"). */
@Composable
fun SheetNote(text: String) {
    Text(
        text,
        style = webText(13f, FontWeight.Bold, AmberInk),
        modifier = Modifier
            .clip(CircleShape)
            .background(Amber)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** `.sheet-props` -- a white card of label/value rows; rows with an empty value are skipped. */
@Composable
fun SheetProps(rows: List<Pair<String, String?>>) {
    val visible = rows.filter { !it.second.isNullOrEmpty() }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(LedgerShapes.medium)
            .background(White)
            .webBorder(HairlineLt, LedgerShapes.medium),
    ) {
        visible.forEachIndexed { index, (key, value) ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = HairlineLt)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(key, style = webText(13f, FontWeight.SemiBold, TextMuted))
                Text(
                    value.orEmpty(),
                    style = webText(14f, FontWeight.Bold, TextDark),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** `.sheet-actions` -- two buttons, 50/50, 10px apart. */
@Composable
fun SheetActions(start: @Composable (Modifier) -> Unit, end: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        start(Modifier.weight(1f))
        end(Modifier.weight(1f))
    }
}
