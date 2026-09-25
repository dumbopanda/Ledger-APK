package com.dumbopanda.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.feature.auth.SignInRoute
import com.dumbopanda.ledger.feature.entry.EntryRoute
import dagger.hilt.android.AndroidEntryPoint

// Temporary entry point. Real boot routing (§7.1's six-row table) and the
// three-tab nav (§7.0, Add Payment | History | Stats) are C9's job, once
// Onboarding, Restoring and Home all actually exist to route between --
// this just makes each screen reachable as it's built, same as A13 did
// for sign-in alone. EntryRoute added once C1's screen assembly landed.
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

@Composable
private fun LedgerApp() {
    var signedIn by remember { mutableStateOf(false) }

    if (signedIn) {
        EntryRoute()
    } else {
        SignInRoute(onSignedIn = { signedIn = true })
    }
}
