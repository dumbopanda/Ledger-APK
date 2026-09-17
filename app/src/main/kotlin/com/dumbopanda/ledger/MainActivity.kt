package com.dumbopanda.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.feature.auth.SignInRoute
import dagger.hilt.android.AndroidEntryPoint

// Temporary entry point for A13's real-phone sign-in verification only.
// Real boot routing (§7.1's six-row table) is C9's job, once Onboarding,
// Restoring and Home all actually exist to route between.
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
        Surface {
            Text("Signed in — profiles row should exist in Supabase now.")
        }
    } else {
        SignInRoute(onSignedIn = { signedIn = true })
    }
}
