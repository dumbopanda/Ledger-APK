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
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState is SignInUiState.Success) {
        onSignedIn()
    }

    SignInScreen(
        uiState = uiState,
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
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
