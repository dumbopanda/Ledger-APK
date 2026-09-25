package com.dumbopanda.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.feature.accounts.AccountsRoute
import com.dumbopanda.ledger.feature.auth.SignInRoute
import com.dumbopanda.ledger.feature.entry.EntryRoute
import com.dumbopanda.ledger.feature.history.HistoryRoute
import dagger.hilt.android.AndroidEntryPoint

// Temporary entry point. Real boot routing (§7.1's six-row table) and the
// three-tab nav (§7.0, Add Payment | History | Stats) are C9's job, once
// Onboarding, Restoring and Home all actually exist to route between --
// this just makes each screen reachable as it's built, same as A13 did
// for sign-in alone. Stats has no screen yet (not in any C-block BUILD-LOG
// names); the switch below covers the three tabs that do exist so far.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LedgerTheme {
                LedgerApp()
            }
        }
    }
}

private enum class TempTab { ADD, HISTORY, ACCOUNTS }

@Composable
private fun LedgerApp() {
    var signedIn by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(TempTab.ADD) }

    if (signedIn) {
        Column(Modifier.fillMaxSize()) {
            LedgerSegmentedButton(
                options = listOf(TempTab.ADD, TempTab.HISTORY, TempTab.ACCOUNTS),
                selected = tab,
                onSelect = { tab = it },
                label = {
                    when (it) {
                        TempTab.ADD -> "Add Payment"
                        TempTab.HISTORY -> "History"
                        TempTab.ACCOUNTS -> "Accounts"
                    }
                },
            )
            Column(Modifier.weight(1f)) {
                when (tab) {
                    TempTab.ADD -> EntryRoute()
                    TempTab.HISTORY -> HistoryRoute(onEditRequested = { tab = TempTab.ADD })
                    TempTab.ACCOUNTS -> AccountsRoute()
                }
            }
        }
    } else {
        SignInRoute(onSignedIn = { signedIn = true })
    }
}
