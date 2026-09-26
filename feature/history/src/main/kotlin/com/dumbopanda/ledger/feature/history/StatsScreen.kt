package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.accountTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.domain.formatMoney
import com.dumbopanda.ledger.core.domain.monthName
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun StatsRoute(
    header: @Composable () -> Unit,
    monthOffset: Int,
    monthToggle: @Composable () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(monthOffset) { viewModel.setMonthOffset(monthOffset) }
    StatsScreen(uiState, header, monthToggle, viewModel::onBreakdownChosen, viewModel::onShowAllToggled)
}

@Composable
internal fun StatsScreen(
    state: StatsUiState,
    header: @Composable () -> Unit,
    monthToggle: @Composable () -> Unit,
    onBreakdown: (Breakdown) -> Unit,
    onShowAll: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        header()
        // `.stats-screen { padding: 14px 20px 32px; gap: 16px }`
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            monthToggle()
            if (!state.hasEntries) {
                if (state.isCurrentMonth) {
                    EmptyCard("Nothing this month yet", "Add an entry and it shows up here.")
                } else {
                    EmptyCard("Nothing in ${monthName(state.month)}", "No entries were added for that month.")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ui-spec.md (APK): 1-3 accounts get a tile each; 4+ fold into one "Accounts"
                    // card so Total spend / Total have stay pinned where they always are.
                    if (state.balances.size <= 3) BalanceGrid(state.balances) else AccountsPanel(state.balances)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Total spend", state.totalSpendMinor, Modifier.weight(1f))
                        StatTile("Total have", state.totalHaveMinor, Modifier.weight(1f), dark = true)
                    }
                    BreakdownPanel(state, onBreakdown, onShowAll)
                    FactsPanel(state.facts)
                }
            }
        }
    }
}

// `.stat-grid` -- two columns, 12px apart; an odd last tile keeps its half width.
@Composable
private fun BalanceGrid(tiles: List<BalanceTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { tile ->
                    StatTile(tile.name, tile.minor, Modifier.weight(1f), dotPosition = tile.position, sub = "current balance")
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun signedRupees(minor: Long): String = (if (minor < 0) "−" else "") + "₹" + formatMoney(kotlin.math.abs(minor))

/** `.stat-tile` (and `.stat-tile.dark`) -- key with its account dot, 21/800 value, optional sub. */
@Composable
private fun StatTile(label: String, minor: Long, modifier: Modifier = Modifier, dark: Boolean = false, dotPosition: Int? = null, sub: String? = null) {
    Column(
        modifier
            .clip(LedgerShapes.medium)
            .background(if (dark) CanvasDark else White)
            .webBorder(if (dark) CanvasDark else HairlineStrong, LedgerShapes.medium)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dotPosition != null) Box(Modifier.size(10.dp).clip(CircleShape).background(accountTone(dotPosition).fill))
            Text(label, style = webText(12f, FontWeight.Bold, if (dark) MutedOnDark else TextMuted, letterSpacing = 0.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            signedRupees(minor),
            style = webText(21f, FontWeight.ExtraBold, if (dark) Lime else if (minor < 0) Coral else TextDark, letterSpacing = -0.4f),
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (sub != null) Text(sub, style = webText(11f, FontWeight.SemiBold, TextMuted), modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(LedgerShapes.medium)
            .background(White)
            .webBorder(HairlineStrong, LedgerShapes.medium),
    ) { content() }
}

// ui-spec.md's "Accounts ▾" -- the 4+-accounts fold, each balance shown on expand.
@Composable
private fun AccountsPanel(tiles: List<BalanceTile>) {
    var open by remember { mutableStateOf(false) }
    Panel {
        Row(
            Modifier.fillMaxWidth().webClickable { open = !open }.padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Accounts", style = webText(15f, FontWeight.ExtraBold, letterSpacing = -0.2f))
            Text(if (open) "⌃" else "⌄", style = webText(14f, FontWeight.ExtraBold, TextMuted))
        }
        if (open) {
            tiles.forEach { tile ->
                HorizontalDivider(thickness = 1.dp, color = HairlineLt)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accountTone(tile.position).fill))
                    Text(tile.name, style = webText(13f, FontWeight.SemiBold, TextMuted), modifier = Modifier.weight(1f))
                    Text(signedRupees(tile.minor), style = webText(13.5f, FontWeight.ExtraBold, if (tile.minor < 0) Coral else TextDark))
                }
            }
        }
    }
}

// `.panel` "Where it went" -- one card, three ways of cutting the same spending.
@Composable
private fun BreakdownPanel(state: StatsUiState, onBreakdown: (Breakdown) -> Unit, onShowAll: () -> Unit) {
    val groups = state.groups
    val shown = if (state.showAllGroups) groups else groups.take(BREAKDOWN_PREVIEW)
    val total = groups.sumOf { it.totalMinor }
    val top = shown.firstOrNull()?.totalMinor?.takeIf { it > 0 } ?: 1L
    Panel {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Where it went", style = webText(15f, FontWeight.ExtraBold, letterSpacing = -0.2f))
            Text("${groups.size} ${if (groups.size == 1) "group" else "groups"}", style = webText(12f, FontWeight.SemiBold, TextMuted))
        }
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(CanvasLight)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Breakdown.entries.forEach { choice ->
                LedgerPill(choice.label, state.breakdown == choice, { onBreakdown(choice) }, Modifier.weight(1f), PillStyles.Segment)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (shown.isEmpty()) {
                Text(
                    "Nothing spent in this month yet.",
                    style = webText(11f, FontWeight.SemiBold, TextMuted),
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                )
            }
            shown.forEachIndexed { index, group ->
                val pct = if (total > 0) (group.totalMinor * 100.0 / total).roundToInt() else 0
                val fill = max(2, (group.totalMinor * 100.0 / top).roundToInt()).coerceAtMost(100) / 100f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(group.name, style = webText(13.5f, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("₹${formatMoney(group.totalMinor)}", style = webText(13.5f, FontWeight.ExtraBold))
                    }
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(CanvasLight)) {
                        Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(if (index == 0) TextDark else Forest))
                    }
                    Text("$pct% · ${if (group.count == 1) "1 entry" else "${group.count} entries"}", style = webText(11f, FontWeight.SemiBold, TextMuted))
                }
            }
        }
        if (groups.size > BREAKDOWN_PREVIEW) {
            HorizontalDivider(thickness = 1.dp, color = HairlineLt)
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (state.showAllGroups) "Show less" else "Show all ${groups.size}",
                    style = webText(13f, FontWeight.Bold, Forest),
                    modifier = Modifier.webClickable(onClick = onShowAll),
                )
            }
        }
    }
}

// `.facts` -- "the small derived numbers that do not deserve a tile each".
@Composable
private fun FactsPanel(facts: List<Pair<String, String>>) {
    Panel {
        facts.forEachIndexed { index, (key, value) ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = HairlineLt)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(key, style = webText(13f, FontWeight.SemiBold, TextMuted))
                Text(value, style = webText(13.5f, FontWeight.ExtraBold), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}
