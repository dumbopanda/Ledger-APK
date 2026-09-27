package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.data.repository.EntrySyncMark
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.AmberDeepInk
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.EditedFill
import com.dumbopanda.ledger.core.designsystem.EditedInk
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.NetOutOnDark
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.accountTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerSubTabs
import com.dumbopanda.ledger.core.designsystem.component.LocalLedgerToast
import com.dumbopanda.ledger.core.designsystem.component.SubTabMark
import com.dumbopanda.ledger.core.domain.DayGroup
import com.dumbopanda.ledger.core.domain.HistoryRow
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.dayLabel
import com.dumbopanda.ledger.core.domain.foldTransfers
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.formatMoney
import com.dumbopanda.ledger.core.domain.monthName
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection
import java.time.YearMonth

/**
 * History: [header] (title bar + tabs) scrolls away with the list, exactly as the web page does;
 * [monthToggle] is the one control History shares with Stats, drawn here above the sub-tabs.
 */
@Composable
fun HistoryRoute(
    header: @Composable () -> Unit,
    monthOffset: Int,
    monthToggle: @Composable () -> Unit,
    onEditRequested: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalLedgerToast.current
    LaunchedEffect(monthOffset) { viewModel.setMonthOffset(monthOffset) }
    LaunchedEffect(viewModel) { viewModel.events.collect { toast.show(it) } }
    HistoryScreen(
        uiState = uiState,
        header = header,
        monthToggle = monthToggle,
        onAction = { action ->
            viewModel.onAction(action)
            if (action == HistoryAction.EditTapped) onEditRequested()
        },
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    header: @Composable () -> Unit,
    monthToggle: @Composable () -> Unit,
    onAction: (HistoryAction) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { header() }
        // `.stats-screen { padding: 14px 20px 32px; gap: 16px }`
        item {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                monthToggle()
                LedgerSubTabs(
                    options = listOf(HistoryTab.RECENT, HistoryTab.OWED_TO_ME),
                    selected = uiState.tab,
                    onSelect = { onAction(HistoryAction.TabSelected(it)) },
                    label = { if (it == HistoryTab.RECENT) "Recent" else "Owed to me" },
                    mark = { tab ->
                        when (tab) {
                            HistoryTab.RECENT -> uiState.recentCount.takeIf { it > 0 }?.let { SubTabMark.Count(it) }
                            HistoryTab.OWED_TO_ME -> uiState.owedToMeOpen.size.takeIf { it > 0 }?.let { SubTabMark.Badge(it) }
                        }
                    },
                )
            }
        }
        when (uiState.tab) {
            HistoryTab.RECENT -> recent(uiState, onAction)
            HistoryTab.OWED_TO_ME -> owedToMe(uiState, onAction)
        }
    }
    uiState.detail?.let { EntryDetailSheet(it, uiState.accounts, uiState.syncMarks, onAction) }
}

private val paneItem = Modifier.fillMaxWidth().padding(horizontal = 20.dp)

private fun androidx.compose.foundation.lazy.LazyListScope.recent(state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    if (state.dayGroups.isEmpty()) {
        item { Column(paneItem) { EmptyMonth(state.month, state.today.let { YearMonth.from(it) } == state.month) } }
        return
    }
    // `.day-list { gap: 18px }` -- the gap between days is bigger than any gap inside one.
    itemsIndexed(state.dayGroups, key = { _, group -> group.date.toString() }) { index, group ->
        Column(paneItem.padding(top = if (index == 0) 0.dp else 18.dp)) { DayCard(group, state, onAction) }
    }
    if (state.hasMore) {
        item {
            Column(paneItem) {
                ShowMoreButton("Show earlier in ${monthName(state.month)}", { onAction(HistoryAction.ShowEarlierTapped) })
            }
        }
    }
}

@Composable
private fun EmptyMonth(month: YearMonth, isCurrent: Boolean) {
    if (isCurrent) {
        EmptyCard("Nothing this month yet", "Add an entry and it shows up here.")
    } else {
        EmptyCard("Nothing in ${monthName(month)}", "No entries were added for that month.")
    }
}

@Composable
private fun DayCard(group: DayGroup, state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    val rows = foldTransfers(group.entries)
    val net = group.netMinor
    LedgerDayCard {
        DayHead(
            label = dayLabel(group.date, state.today),
            figure = when {
                net > 0 -> "+ ₹${formatMoney(net)}"
                net < 0 -> "− ₹${formatMoney(-net)}"
                else -> ""
            },
            figureColor = when {
                net > 0 -> Lime
                net < 0 -> NetOutOnDark
                else -> MutedOnDark
            },
        )
        rows.forEachIndexed { index, row ->
            val last = index == rows.lastIndex
            when (row) {
                is HistoryRow.Single -> SingleRow(row.entry, state, !last) { onAction(HistoryAction.EntryTapped(row.entry)) }
                is HistoryRow.Transfer -> TransferRow(row.sent, row.received, state, !last) { onAction(HistoryAction.EntryTapped(row.sent)) }
            }
        }
    }
}

private fun tileFor(account: Account?, order: Map<String, Int>): RowTile {
    val tone = accountTone(order[account?.id] ?: 0)
    return RowTile(account?.name?.firstOrNull()?.uppercase() ?: "?", tone.fill)
}

private fun accountTag(text: String, account: Account?, order: Map<String, Int>): RowTag {
    val tone = accountTone(order[account?.id] ?: 0)
    return RowTag(text, tone.fill, tone.ink)
}

private fun syncTags(marks: Collection<EntrySyncMark?>): List<RowTag> = buildList {
    if (EntrySyncMark.OnDevice in marks) add(RowTag("on this device", Amber, AmberDeepInk))
    if (EntrySyncMark.Edited in marks) add(RowTag("edited", EditedFill, EditedInk))
}

@Composable
private fun SingleRow(entry: Entry, state: HistoryUiState, divider: Boolean, onClick: () -> Unit) {
    val account = state.accounts[entry.accountId]
    val out = entry.direction == TxnDirection.Debit
    val tags = buildList {
        add(accountTag(account?.name.orEmpty(), account, state.accountOrder))
        if (entry.txnType.isNotBlank()) add(RowTag(entry.txnType))
        if (entry.txnId.isY) add(RowTag("Owed"))
        addAll(syncTags(listOf(state.syncMarks[entry.clientUid])))
    }
    TxnRow(
        tile = tileFor(account, state.accountOrder),
        name = entry.expenseName.ifBlank { "Untitled" },
        amount = (if (out) "− " else "+ ") + "₹" + formatMoney(entry.amount.minor),
        amountColor = if (out) Coral else Forest,
        tags = tags,
        note = entry.moreInfo,
        onClick = onClick,
        showDivider = divider,
    )
}

@Composable
private fun TransferRow(sent: Entry, received: Entry, state: HistoryUiState, divider: Boolean, onClick: () -> Unit) {
    val from = state.accounts[sent.accountId]
    val to = state.accounts[received.accountId]
    val tags = buildList {
        add(accountTag("${from?.name.orEmpty()} → ${to?.name.orEmpty()}", from, state.accountOrder))
        if (sent.txnType.isNotBlank()) add(RowTag(sent.txnType))
        addAll(syncTags(listOf(state.syncMarks[sent.clientUid], state.syncMarks[received.clientUid])))
    }
    TxnRow(
        tile = RowTile("⇄", White, TextDark, border = HairlineStrong),
        name = "Self transfer",
        amount = "₹" + formatMoney(sent.amount.minor),
        amountColor = TextMuted,
        tags = tags,
        note = sent.moreInfo,
        onClick = onClick,
        showDivider = divider,
    )
}

// "One list, one place to look -- open items on top in their own card, closed ones below in
// theirs, grayed out. Never two tabs to check."
private fun androidx.compose.foundation.lazy.LazyListScope.owedToMe(state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    if (state.owedToMeOpen.isEmpty() && state.owedToMeClosed.isEmpty()) {
        item {
            Column(paneItem) {
                EmptyCard("Nothing here yet", "Anything you mark \"Yes, I'll get this back\" shows up here, open or settled.")
            }
        }
        return
    }
    if (state.owedToMeOpen.isNotEmpty()) {
        item { Column(paneItem) { OwedCard("Open", state.owedToMeOpen, closed = false, state, onAction) } }
    }
    if (state.owedToMeClosed.isNotEmpty()) {
        item {
            Column(paneItem.padding(top = if (state.owedToMeOpen.isEmpty()) 0.dp else 18.dp)) {
                OwedCard("Closed", state.owedToMeClosed, closed = true, state, onAction)
            }
        }
    }
}

@Composable
private fun OwedCard(label: String, items: List<OwedToMeItem>, closed: Boolean, state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    LedgerDayCard {
        DayHead(label = label, figure = items.size.toString())
        items.forEachIndexed { index, item ->
            val account = state.accounts[item.accountId]
            val dates = if (closed) {
                "Opened ${formatDisplayDate(item.openedDate)} · Closed ${item.closedDate?.let(::formatDisplayDate).orEmpty()}"
            } else {
                "Opened ${formatDisplayDate(item.openedDate)}"
            }
            val tile = if (closed) {
                RowTile(account?.name?.firstOrNull()?.uppercase() ?: "?", CanvasLight, TextMuted, border = HairlineLt)
            } else {
                tileFor(account, state.accountOrder)
            }
            val acctTag = if (closed) RowTag(account?.name.orEmpty()) else accountTag(account?.name.orEmpty(), account, state.accountOrder)
            TxnRow(
                tile = tile,
                name = item.expenseName,
                amount = "₹" + formatMoney(item.amount.minor),
                amountColor = TextDark,
                tags = listOf(acctTag, RowTag(dates)),
                note = null,
                onClick = { onAction(HistoryAction.OwedItemTapped(item)) },
                showDivider = index != items.lastIndex,
                dimmed = closed,
            )
        }
    }
}
