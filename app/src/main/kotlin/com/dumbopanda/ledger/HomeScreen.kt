package com.dumbopanda.ledger

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.data.sync.SyncMode
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.Mint
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.UpdateInk
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.LedgerBarButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerMonthToggle
import com.dumbopanda.ledger.core.designsystem.component.LedgerNoticeBar
import com.dumbopanda.ledger.core.designsystem.component.LedgerProfileButton
import com.dumbopanda.ledger.core.designsystem.component.LedgerTopTabs
import com.dumbopanda.ledger.core.designsystem.component.LocalLedgerToast
import com.dumbopanda.ledger.core.designsystem.component.webClickable
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.monthLabel
import com.dumbopanda.ledger.core.domain.monthSub
import com.dumbopanda.ledger.feature.accounts.AccountsRoute
import com.dumbopanda.ledger.feature.entry.EntryRoute
import com.dumbopanda.ledger.feature.history.HistoryRoute
import com.dumbopanda.ledger.feature.history.StatsRoute
import com.dumbopanda.ledger.feature.settings.SettingsRoute
import java.time.LocalDate
import java.time.YearMonth

// The three tabs are index.html's own; Settings (and Accounts inside it) live behind the
// profile button in the title bar, exactly where the web keeps its account screen.
private enum class Screen { ADD, HISTORY, STATS, SETTINGS, ACCOUNTS }

private val TABS = listOf(Screen.ADD, Screen.HISTORY, Screen.STATS)

// "Never positive -- there is no such thing as a future entry"; and no further back than this.
private const val MONTHS_BACK_MAX = 24

@Composable
fun HomeScreen(
    sessionExpiredNote: Boolean,
    onSignedOut: () -> Unit,
    onDeleteAccountRequested: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val home by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalLedgerToast.current
    LaunchedEffect(viewModel) { viewModel.events.collect { toast.show(it) } }

    var screen by rememberSaveable { mutableStateOf(Screen.ADD) }
    // One month offset drives both History and Stats, so they never describe different periods.
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = screen != Screen.ADD) {
        screen = if (screen == Screen.ACCOUNTS) Screen.SETTINGS else Screen.ADD
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(uri)
    }

    val header: @Composable () -> Unit = {
        AppHeader(
            screen = screen,
            home = home,
            sessionExpiredNote = sessionExpiredNote,
            onProfile = { screen = Screen.SETTINGS },
            onTab = { screen = it },
            onSendNow = viewModel::sendNow,
            onBackupLater = viewModel::snoozeBackup,
            onBackupNow = { exportLauncher.launch("ledger-backup-${LocalDate.now()}.json") },
        )
    }
    val monthToggle: @Composable () -> Unit = {
        LedgerMonthToggle(
            label = monthLabel(YearMonth.now().plusMonths(monthOffset.toLong())),
            sub = monthSub(monthOffset),
            prevEnabled = monthOffset > -MONTHS_BACK_MAX,
            nextEnabled = monthOffset < 0,
            onPrev = { if (monthOffset > -MONTHS_BACK_MAX) monthOffset -= 1 },
            onNext = { if (monthOffset < 0) monthOffset += 1 },
        )
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            Screen.ADD -> EntryRoute(header = header, onEditSaved = { screen = Screen.HISTORY })
            Screen.HISTORY -> HistoryRoute(header, monthOffset, monthToggle, onEditRequested = { screen = Screen.ADD })
            Screen.STATS -> StatsRoute(header, monthOffset, monthToggle)
            Screen.SETTINGS -> SettingsRoute(
                header = header,
                onAccountsRequested = { screen = Screen.ACCOUNTS },
                onSignedOut = onSignedOut,
                onDeleteAccountRequested = onDeleteAccountRequested,
            )
            Screen.ACCOUNTS -> AccountsRoute(header = header, onBack = { screen = Screen.SETTINGS })
        }
    }
}

/**
 * Everything above a screen's own content, in index.html's order: the title bar, the session
 * note, the nightly backup bar, the three tabs, and -- on Add Payment only -- the sync bar.
 */
@Composable
private fun AppHeader(
    screen: Screen,
    home: HomeUiState,
    sessionExpiredNote: Boolean,
    onProfile: () -> Unit,
    onTab: (Screen) -> Unit,
    onSendNow: () -> Unit,
    onBackupLater: () -> Unit,
    onBackupNow: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // `.topbar { padding: 22px 20px 14px }`
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ledger", style = webText(22f, FontWeight.ExtraBold, letterSpacing = -0.3f))
                Text(
                    formatDisplayDate(LocalDate.now()),
                    style = webText(13f, FontWeight.Medium, TextMuted),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            LedgerProfileButton(
                active = screen == Screen.SETTINGS || screen == Screen.ACCOUNTS,
                pendingCount = home.pendingCount,
                onClick = onProfile,
            )
        }

        if (sessionExpiredNote) {
            LedgerNoticeBar(Amber, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
                Text("Signed out. Your entries are safe on this device.", style = webText(13f, FontWeight.Bold, UpdateInk), modifier = Modifier.weight(1f))
            }
        }

        if (home.showBackupBar) {
            LedgerNoticeBar(Mint, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
                Text("Keep a copy in your own files too?", style = webText(13f, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                Text(
                    "Later",
                    style = webText(13f, FontWeight.Bold, TextMuted),
                    modifier = Modifier.webClickable(onClick = onBackupLater).padding(horizontal = 4.dp, vertical = 8.dp),
                )
                LedgerBarButton("Save file", onBackupNow, Forest, White)
            }
        }

        // `.top-tabs-wrap { padding: 4px 20px }`
        LedgerTopTabs(
            options = TABS,
            selected = screen.takeIf { it in TABS },
            onSelect = onTab,
            label = {
                when (it) {
                    Screen.ADD -> "Add Payment"
                    Screen.HISTORY -> "History"
                    else -> "Stats"
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (screen == Screen.ADD && home.pendingCount > 0) SyncBar(home, onSendNow)
    }
}

// `.sync-bar` -- "only present when something is actually waiting". Offline is a normal state
// here, not a failure: the entry is already safe on the device.
@Composable
private fun SyncBar(home: HomeUiState, onSendNow: () -> Unit) {
    val n = home.pendingCount
    val changes = if (n == 1) "1 change" else "$n changes"
    val label = when {
        !home.online -> "$changes not backed up yet. You're offline."
        home.syncMode == SyncMode.MANUAL -> "$changes not backed up yet."
        else -> "$changes not backed up yet. Going up shortly."
    }
    LedgerNoticeBar(
        fill = if (home.online) Mint else CanvasLight,
        border = if (home.online) null else HairlineLt,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
    ) {
        Box(Modifier.size(8.dp).background(if (home.online) Forest else TextMuted, CircleShape))
        Text(label, style = webText(13f, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        LedgerBarButton(
            text = if (home.online) "Back up now" else "Offline",
            onClick = onSendNow,
            fill = Forest,
            ink = White,
            enabled = home.online,
        )
    }
}
