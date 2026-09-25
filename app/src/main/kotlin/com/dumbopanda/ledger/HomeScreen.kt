package com.dumbopanda.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.feature.accounts.AccountsRoute
import com.dumbopanda.ledger.feature.entry.EntryRoute
import com.dumbopanda.ledger.feature.history.HistoryRoute
import com.dumbopanda.ledger.feature.settings.SettingsRoute

// Four tabs, not the three-tab (Add Payment | History | Stats) shape ui-spec.md describes:
// Stats has no screen anywhere in this repo yet (not in any C-block, flagged as unscheduled
// since S2) -- inventing one here would be scope creep C9's own checklist doesn't ask for.
// This keeps A13's four working tabs (Add/History/Accounts/Settings) reachable behind the
// real boot-routed NavHost instead of MainActivity's old hardcoded `signedIn` boolean; the
// tab-bar reshape to match ui-spec.md is still open, tracked wherever Stats itself gets built.
private enum class HomeTab { ADD, HISTORY, ACCOUNTS, SETTINGS }

@Composable
fun HomeScreen(
    sessionExpiredNote: Boolean,
    onSignedOut: () -> Unit,
) {
    var tab by remember { mutableStateOf(HomeTab.ADD) }

    Column(Modifier.fillMaxSize()) {
        if (sessionExpiredNote) {
            LedgerBanner(
                text = "Signed out. Your entries are safe on this device.",
                tone = LedgerBannerTone.Warning,
            )
        }
        LedgerSegmentedButton(
            options = listOf(HomeTab.ADD, HomeTab.HISTORY, HomeTab.ACCOUNTS, HomeTab.SETTINGS),
            selected = tab,
            onSelect = { tab = it },
            label = {
                when (it) {
                    HomeTab.ADD -> "Add Payment"
                    HomeTab.HISTORY -> "History"
                    HomeTab.ACCOUNTS -> "Accounts"
                    HomeTab.SETTINGS -> "Settings"
                }
            },
        )
        Column(Modifier.weight(1f)) {
            when (tab) {
                HomeTab.ADD -> EntryRoute()
                HomeTab.HISTORY -> HistoryRoute(onEditRequested = { tab = HomeTab.ADD })
                HomeTab.ACCOUNTS -> AccountsRoute()
                HomeTab.SETTINGS -> SettingsRoute(
                    onAccountsRequested = { tab = HomeTab.ACCOUNTS },
                    onSignedOut = onSignedOut,
                )
            }
        }
    }
}
