package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * `.ledger-card` -- "a day is one object": white, the heavier `--border-strong` rule, a soft
 * two-part shadow, contents clipped to the 22px corners.
 */
@Composable
fun LedgerDayCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(3.dp, LedgerShapes.medium, ambientColor = TextDark.copy(alpha = 0.05f), spotColor = TextDark.copy(alpha = 0.18f))
            .clip(LedgerShapes.medium)
            .background(White)
            .webBorder(HairlineStrong, LedgerShapes.medium),
        content = content,
    )
}

/** `.day-head` -- dark band: 12/800 uppercase lime label, 13/700 figure on the right. */
@Composable
fun DayHead(label: String, figure: String, figureColor: Color = MutedOnDark) {
    Row(
        Modifier.fillMaxWidth().background(CanvasDark).padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = webText(12f, FontWeight.ExtraBold, Lime, letterSpacing = 0.6f))
        Text(figure, style = webText(13f, FontWeight.Bold, figureColor))
    }
}

/** One `.tag` pill -- 11/700, 3/9 padding. */
data class RowTag(val text: String, val fill: Color = CanvasLight, val ink: Color = TextMuted)

/** The `.tile` at the left of every row. */
data class RowTile(val mark: String, val fill: Color, val ink: Color = TextDark, val border: Color? = null)

/**
 * `.txn` -- "two lines instead of one": the name (2 lines max) shares the top line with the
 * amount; account, category, owed-ness and sync state wrap as tags on the line below; the note
 * sits under those. The whole row darkens to cream while pressed, like `.txn.tappable:active`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TxnRow(
    tile: RowTile,
    name: String,
    amount: String?,
    amountColor: Color,
    tags: List<RowTag>,
    note: String?,
    onClick: (() -> Unit)?,
    showDivider: Boolean,
    dimmed: Boolean = false,
    nameColor: Color = TextDark,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(Modifier.alpha(if (dimmed) 0.55f else 1f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (pressed) CanvasLight else Color.Transparent)
                .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .padding(top = 1.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tile.fill)
                    .then(if (tile.border != null) Modifier.border(1.5.dp, tile.border, RoundedCornerShape(12.dp)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(tile.mark, style = webText(15f, FontWeight.ExtraBold, tile.ink))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        name,
                        style = webText(15f, FontWeight.Bold, nameColor, lineHeight = 15f * 1.3f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (amount != null) {
                        Text(amount, style = webText(15f, FontWeight.ExtraBold, amountColor, lineHeight = 15f * 1.3f), maxLines = 1)
                    }
                }
                if (tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag -> Tag(tag) }
                    }
                }
                if (!note.isNullOrBlank()) {
                    Text(note, style = webText(12.5f, FontWeight.Medium, TextMuted, lineHeight = 12.5f * 1.35f))
                }
            }
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = HairlineLt)
    }
}

@Composable
private fun Tag(tag: RowTag) {
    Text(
        tag.text,
        style = webText(11f, FontWeight.Bold, tag.ink),
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(tag.fill).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** `.stats-empty` inside a `.ledger-card` -- what a month or list with nothing in it says. */
@Composable
fun EmptyCard(title: String, body: String) {
    LedgerDayCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = webText(17f, FontWeight.Bold), textAlign = TextAlign.Center)
            Text(
                body,
                style = webText(14f, FontWeight.Medium, TextMuted, lineHeight = 21f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** `.show-more` -- a full-width white pill, 1px border-strong, 14/700. */
@Composable
fun ShowMoreButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(White)
            .webBorder(HairlineStrong, CircleShape)
            .webClickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = webText(14f, FontWeight.Bold))
    }
}
