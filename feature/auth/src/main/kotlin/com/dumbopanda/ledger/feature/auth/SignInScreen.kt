package com.dumbopanda.ledger.feature.auth

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect

/**
 * The SIGN_IN / SIGN_IN_EXPIRED destination: one screen for both ways in. Email (a 6-digit code,
 * [EmailSignInViewModel]) leads; Google ([SignInViewModel], the same Credential Manager flow as
 * before) sits under it. Sign in vs Sign up is only framing -- the first verified code creates
 * the account either way -- so [AuthMode] lives here as UI state, not in either view model.
 */
@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
    sessionExpiredNote: Boolean = false,
    viewModel: SignInViewModel = hiltViewModel(),
    emailViewModel: EmailSignInViewModel = hiltViewModel(),
) {
    val googleState by viewModel.uiState.collectAsStateWithLifecycle()
    val emailState by emailViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Sign up leads for everyone arriving fresh. Only a session-expired return -- someone who
    // was signed in on this very phone minutes ago -- opens on Sign in.
    var mode by rememberSaveable { mutableStateOf(if (sessionExpiredNote) AuthMode.SignIn else AuthMode.SignUp) }

    SystemChromeEffect(SystemChrome.DarkOverCream)
    // Back from the code screen returns to the email screen, not out of sign-in.
    BackHandler(enabled = emailState is EmailSignInUiState.EnteringCode) { emailViewModel.changeEmail() }

    // Gated to fire once per Success transition: onSignedIn navigates, and an un-gated call on
    // every recomposition would push duplicate back-stack entries.
    LaunchedEffect(googleState) {
        if (googleState is SignInUiState.Success) onSignedIn()
    }
    LaunchedEffect(emailState) {
        if (emailState is EmailSignInUiState.Success) onSignedIn()
    }

    AuthScreen(
        mode = mode,
        googleState = googleState,
        emailState = emailState,
        sessionExpiredNote = sessionExpiredNote,
        onModeChange = { mode = it },
        onNameChanged = emailViewModel::onNameChanged,
        onEmailChanged = emailViewModel::onEmailChanged,
        onSendCode = { emailViewModel.sendCode(signUp = mode == AuthMode.SignUp) },
        onCodeChanged = emailViewModel::onCodeChanged,
        onVerify = emailViewModel::verifyCode,
        onResend = emailViewModel::resendCode,
        onChangeEmail = emailViewModel::changeEmail,
        onContinueWithGoogle = { viewModel.signIn(context) },
        onOpenAddAccountSettings = { context.startActivity(Intent(Settings.ACTION_ADD_ACCOUNT)) },
    )
}
