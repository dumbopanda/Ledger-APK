package com.dumbopanda.ledger

import android.os.Bundle
import android.view.WindowManager
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerSegmentedButton
import com.dumbopanda.ledger.feature.accounts.AccountsRoute
import com.dumbopanda.ledger.feature.auth.SignInRoute
import com.dumbopanda.ledger.feature.entry.EntryRoute
import com.dumbopanda.ledger.feature.history.HistoryRoute
import com.dumbopanda.ledger.feature.settings.SettingsRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Temporary entry point. Real boot routing (§7.1's six-row table) and the
// three-tab nav (§7.0, Add Payment | History | Stats) are C9's job, once
// Onboarding, Restoring and Home all actually exist to route between --
// this just makes each screen reachable as it's built, same as A13 did
// for sign-in alone. Stats has no screen yet (not in any C-block BUILD-LOG
// names); the switch below covers the four tabs that do exist so far.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userPrefsRepository: UserPrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // §7.13's "Hide amounts in the app switcher" -- the one security toggle actually
        // enforced end to end (the other, biometric-to-open, has nowhere to gate yet -- see
        // SettingsScreen's own doc). FLAG_SECURE also blocks screenshots as a side effect,
        // which is a reasonable bonus for a finance app, not just a switcher-thumbnail concern.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPrefsRepository.showAmountsInAppSwitcher().collect { show ->
                    if (show) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            LedgerTheme {
                LedgerApp()
            }
        }
    }
}

private enum class TempTab { ADD, HISTORY, ACCOUNTS, SETTINGS }

@Composable
private fun LedgerApp() {
    var signedIn by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(TempTab.ADD) }

    if (signedIn) {
        Column(Modifier.fillMaxSize()) {
            LedgerSegmentedButton(
                options = listOf(TempTab.ADD, TempTab.HISTORY, TempTab.ACCOUNTS, TempTab.SETTINGS),
                selected = tab,
                onSelect = { tab = it },
                label = {
                    when (it) {
                        TempTab.ADD -> "Add Payment"
                        TempTab.HISTORY -> "History"
                        TempTab.ACCOUNTS -> "Accounts"
                        TempTab.SETTINGS -> "Settings"
                    }
                },
            )
            Column(Modifier.weight(1f)) {
                when (tab) {
                    TempTab.ADD -> EntryRoute()
                    TempTab.HISTORY -> HistoryRoute(onEditRequested = { tab = TempTab.ADD })
                    TempTab.ACCOUNTS -> AccountsRoute()
                    TempTab.SETTINGS -> SettingsRoute(
                        onAccountsRequested = { tab = TempTab.ACCOUNTS },
                        onSignedOut = {
                            signedIn = false
                            tab = TempTab.ADD
                        },
                    )
                }
            }
        }
    } else {
        SignInRoute(onSignedIn = { signedIn = true })
    }
}
