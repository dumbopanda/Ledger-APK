package com.dumbopanda.ledger.feature.auth

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton

@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
    sessionExpiredNote: Boolean = false,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // §7.1 row 2's boot-time refresh failure lands here with sessionExpiredNote = true; a
    // fresh sign-in from Onboarding passes false. Was a direct call in the composable body
    // before NavHost wiring existed (harmless when it only flipped a local boolean) -- now
    // that onSignedIn navigates, an un-gated call on every recomposition of Success would
    // push duplicate back-stack entries, so it's gated to fire once per Success transition.
    LaunchedEffect(uiState) {
        if (uiState is SignInUiState.Success) onSignedIn()
    }

    SignInScreen(
        uiState = uiState,
        sessionExpiredNote = sessionExpiredNote,
        onContinueWithGoogle = { viewModel.signIn(context) },
        onOpenAddAccountSettings = {
            context.startActivity(Intent(Settings.ACTION_ADD_ACCOUNT))
        },
    )
}

@Composable
internal fun SignInScreen(
    uiState: SignInUiState,
    onContinueWithGoogle: () -> Unit,
    onOpenAddAccountSettings: () -> Unit,
    sessionExpiredNote: Boolean = false,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // §7.15: "Signed out. Your entries are safe on this device." -- shown alongside
            // Idle/InProgress only; a real error from THIS attempt (NetworkFailure,
            // TokenRejected, ...) is more specific and takes the banner slot instead.
            if (sessionExpiredNote && (uiState is SignInUiState.Idle || uiState is SignInUiState.InProgress)) {
                LedgerBanner(
                    text = "Signed out. Your entries are safe on this device.",
                    tone = LedgerBannerTone.Warning,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            when (uiState) {
                is SignInUiState.NoGoogleAccountOnDevice -> {
                    LedgerBanner(
                        text = "No Google account on this device",
                        tone = LedgerBannerTone.Warning,
                    )
                    LedgerPillButton(
                        text = "Add a Google account",
                        onClick = onOpenAddAccountSettings,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is SignInUiState.NetworkFailure -> {
                    LedgerBanner(text = uiState.message, tone = LedgerBannerTone.Error)
                    LedgerPillButton(
                        text = "Try again",
                        onClick = onContinueWithGoogle,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is SignInUiState.TokenRejected -> {
                    LedgerBanner(text = "Sign-in failed.", tone = LedgerBannerTone.Error)
                    LedgerPillButton(
                        text = "Try again",
                        onClick = onContinueWithGoogle,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is SignInUiState.InProgress -> {
                    CircularProgressIndicator()
                }

                is SignInUiState.Idle, is SignInUiState.Success -> {
                    LedgerPillButton(
                        text = "Continue with Google",
                        onClick = onContinueWithGoogle,
                        enabled = uiState !is SignInUiState.InProgress,
                    )
                }
            }
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun SignInScreenIdlePreview() {
    LedgerTheme {
        SignInScreen(uiState = SignInUiState.Idle, onContinueWithGoogle = {}, onOpenAddAccountSettings = {})
    }
}

@Preview
@Composable
private fun SignInScreenNoAccountPreview() {
    LedgerTheme {
        SignInScreen(
            uiState = SignInUiState.NoGoogleAccountOnDevice,
            onContinueWithGoogle = {},
            onOpenAddAccountSettings = {},
        )
    }
}
