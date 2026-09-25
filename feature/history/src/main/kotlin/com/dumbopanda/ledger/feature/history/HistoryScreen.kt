package com.dumbopanda.ledger.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.dumbopanda.ledger.core.designsystem.component.LedgerCard
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.core.domain.DayGroup
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.GroupEntriesByDayUseCase
import com.dumbopanda.ledger.core.domain.HistoryRow
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.foldTransfers
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_HEADER_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH)
private val OWED_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val amountFormatter = FormatAmountUseCase()

// Rupee text for a possibly-zero-or-negative minor-unit total (a pure-income or pure-transfer
// day/month can net to <= 0) -- Money itself requires > 0, so §7.7's summary tiles format the
// sign and magnitude directly rather than routing through it.
private fun signedRupees(minor: Long): String {
    val sign = if (minor < 0) "-" else ""
    val magnitude = kotlin.math.abs(minor)
    val rupees = magnitude / 100
    val paise = magnitude % 100
    val clean = if (paise == 0L) rupees.toString() else "$rupees.${paise.toString().padStart(2, '0')}"
    return sign + amountFormatter.toDisplay(clean)
}

@Composable
fun HistoryRoute(onEditRequested: () -> Unit = {}, viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedEntries = viewModel.pagedEntries.collectAsLazyPagingItems()
    HistoryScreen(
        uiState = uiState,
        pagedEntries = pagedEntries,
        onAction = { action ->
            viewModel.onAction(action)
            // Handing the entry over happens via EditRequestStore (data layer); this callback
            // is the UI-only "switch to the Add tab" concern MainActivity's temporary switch
            // needs -- C9's real nav graph replaces both with one navigate() call.
            if (action == HistoryAction.EditTapped) onEditRequested()
        },
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    pagedEntries: LazyPagingItems<Entry>,
    onAction: (HistoryAction) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            LedgerSegmentedButton(
                options = listOf(HistoryTab.RECENT, HistoryTab.OWED_TO_ME),
                selected = uiState.tab,
                onSelect = { onAction(HistoryAction.TabSelected(it)) },
                label = { if (it == HistoryTab.RECENT) "Recent" else "Owed to me (${uiState.owedToMeOpen.size})" },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            when (uiState.tab) {
                HistoryTab.RECENT -> RecentTab(uiState, pagedEntries, onAction)
                HistoryTab.OWED_TO_ME -> OwedToMeTab(uiState, onAction)
            }
        }
    }

    uiState.detail?.let { detail -> EntryDetailSheet(state = detail, onAction = onAction) }
}

@Composable
private fun RecentTab(uiState: HistoryUiState, pagedEntries: LazyPagingItems<Entry>, onAction: (HistoryAction) -> Unit) {
    // Materialized once per load rather than driven item-by-item, because grouping (by day)
    // and folding (transfer legs into one row) both need to see neighboring items, which a
    // pure per-index `items(lazyPagingItems.itemCount)` render can't do. §7.7's own cap (40
    // rows per page, "Show earlier entries") keeps this small enough that re-deriving on every
    // page-load is cheap -- this is not re-run per keystroke or per scroll frame.
    val entries = remember(pagedEntries.itemCount) { pagedEntries.itemSnapshotList.toList().filterNotNull() }
    val dayGroups = remember(entries) { GroupEntriesByDayUseCase().group(entries) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { MonthSummaryCard(uiState.monthSummary.outMinor, uiState.monthSummary.inMinor, uiState.monthSummary.netMinor) }
        if (dayGroups.isEmpty()) {
            item {
                Text(
                    "Nothing here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }
        items(dayGroups) { group -> DayCard(group, uiState.accounts, onAction) }
    }
}

@Composable
private fun MonthSummaryCard(outMinor: Long, inMinor: Long, netMinor: Long) {
    LedgerCard(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryTile("Out", outMinor, MaterialTheme.colorScheme.error)
            SummaryTile("In", inMinor, MaterialTheme.colorScheme.primary)
            SummaryTile("Net", netMinor, MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SummaryTile(label: String, minor: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("₹" + signedRupees(minor), style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun DayCard(group: DayGroup, accounts: Map<String, Account>, onAction: (HistoryAction) -> Unit) {
    LedgerCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(group.date.format(DAY_HEADER_FORMAT), style = MaterialTheme.typography.titleSmall)
            Text(
                "₹" + signedRupees(group.netMinor),
                style = MaterialTheme.typography.titleSmall,
                color = if (group.netMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        val rows = remember(group) { foldTransfers(group.entries) }
        Column(Modifier.padding(top = 8.dp)) {
            rows.forEach { row ->
                when (row) {
                    is HistoryRow.Single -> EntryRow(
                        entry = row.entry,
                        account = accounts[row.entry.accountId],
                        onClick = { onAction(HistoryAction.EntryTapped(row.entry)) },
                    )
                    is HistoryRow.Transfer -> TransferRow(
                        sent = row.sent,
                        received = row.received,
                        fromAccount = accounts[row.sent.accountId],
                        toAccount = accounts[row.received.accountId],
                        onClick = { onAction(HistoryAction.EntryTapped(row.sent)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(letter: Char) {
    Box(
        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryRow(entry: Entry, account: Account?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(account?.name?.firstOrNull() ?: '?')
        Column(Modifier.weight(1f)) {
            Text(entry.expenseName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${account?.name.orEmpty()} · ${entry.txnType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val signed = if (entry.direction == TxnDirection.Debit) "− " else "+ "
        Text(
            signed + "₹" + amountFormatter.toDisplay(entry.amount.toCleanRupeeString()),
            style = MaterialTheme.typography.bodyLarge,
            color = if (entry.direction == TxnDirection.Debit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TransferRow(sent: Entry, received: Entry, fromAccount: Account?, toAccount: Account?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile('⇄')
        Column(Modifier.weight(1f)) {
            Text("Self transfer", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${fromAccount?.name.orEmpty()} → ${toAccount?.name.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "₹" + amountFormatter.toDisplay(sent.amount.toCleanRupeeString()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OwedToMeTab(uiState: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("Open", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            LedgerCard {
                if (uiState.owedToMeOpen.isEmpty()) {
                    Text(
                        "Nothing outstanding — entries you mark Y show up here until the money comes back.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column { uiState.owedToMeOpen.forEach { item -> OwedRow(item, uiState.accounts, onAction) } }
                }
            }
        }
        if (uiState.owedToMeClosed.isNotEmpty()) {
            item {
                Text("Closed", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                LedgerCard(modifier = Modifier.alpha(0.55f)) {
                    Column { uiState.owedToMeClosed.forEach { item -> OwedRow(item, uiState.accounts, onAction) } }
                }
            }
        }
    }
}

@Composable
private fun OwedRow(item: OwedToMeItem, accounts: Map<String, Account>, onAction: (HistoryAction) -> Unit) {
    val account = accounts[item.accountId]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(HistoryAction.OwedItemTapped(item)) }
            .padding(vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.expenseName, style = MaterialTheme.typography.bodyLarge)
            Text("₹" + amountFormatter.toDisplay(item.amount.toCleanRupeeString()), style = MaterialTheme.typography.bodyLarge)
        }
        val meta = if (item.isOpen) {
            "${account?.name.orEmpty()} · Opened ${item.openedDate.format(OWED_DATE_FORMAT)}"
        } else {
            "${account?.name.orEmpty()} · Opened ${item.openedDate.format(OWED_DATE_FORMAT)} · " +
                "Closed ${item.closedDate?.format(OWED_DATE_FORMAT).orEmpty()}"
        }
        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
