package com.dumbopanda.ledger

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * §7.13's "Require biometric to open" (default off), finally gating something -- C7's own note
 * said this toggle "persists but gates nothing... the gate belongs in §7.1's Boot routing,
 * which is C9's job." Wraps whatever [content] the boot decision resolved to ([RestoringRoute]
 * or Home); Onboarding/SignIn/ForcedUpdate never pass through here at all.
 *
 * Fails OPEN, not closed, whenever biometric auth simply isn't usable on this device
 * ([BiometricManager.canAuthenticate] not [BiometricManager.BIOMETRIC_SUCCESS]) even though the
 * toggle is on -- e.g. every fingerprint was removed from the OS after the user turned this on.
 * This is an *optional app lock* (§8: "Optional app lock"), not the boundary that protects the
 * user's data (that's the Google account + RLS); permanently locking someone out of their own
 * ledger over a toggle they can no longer satisfy would be a worse failure than the lock doing
 * nothing.
 */
@Composable
fun BiometricGate(
    viewModel: BiometricGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val requireBiometric by viewModel.requireBiometric.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current

    var unlocked by remember { mutableStateOf(false) }
    var promptFailed by remember { mutableStateOf(false) }
    var authenticating by remember { mutableStateOf(false) }
    var stoppedAt by remember { mutableStateOf<Long?>(null) }

    // "Asked each time Ledger opens": coming back after more than a minute away locks again. The
    // PIN screen itself stops this activity, so a stop during authentication doesn't count.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (!authenticating) stoppedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    val away = stoppedAt?.let { SystemClock.elapsedRealtime() - it }
                    if (away != null && away > RELOCK_AFTER_MS) {
                        unlocked = false
                        promptFailed = false
                    }
                    stoppedAt = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val required = requireBiometric
    val activity = context as? FragmentActivity
    val canAuthenticate = activity != null &&
        BiometricManager.from(context).canAuthenticate(APP_LOCK_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

    if (required == null || !required || unlocked || activity == null || !canAuthenticate) {
        content()
        return
    }

    LaunchedEffect(required, promptFailed) {
        if (!promptFailed) {
            authenticating = true
            unlocked = authenticateBiometric(activity, "Unlock Ledger")
            authenticating = false
            if (!unlocked) promptFailed = true
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Ledger is locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            LedgerPillButton(
                text = "Unlock",
                onClick = { promptFailed = false },
            )
        }
    }
}

private suspend fun authenticateBiometric(activity: FragmentActivity, title: String): Boolean =
    suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(APP_LOCK_AUTHENTICATORS)
            .build()
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(promptInfo)
    }

// The setting promises "fingerprint or screen lock": a phone with only a PIN used to get no lock
// at all, because biometric-only auth reported "unavailable" and the gate fails open.
// No negative button is allowed alongside DEVICE_CREDENTIAL; the PIN screen has its own cancel.
private const val APP_LOCK_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
private const val RELOCK_AFTER_MS = 60_000L
