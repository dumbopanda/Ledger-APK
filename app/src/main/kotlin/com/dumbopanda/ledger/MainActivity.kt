package com.dumbopanda.ledger

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// §7.1's boot decision (BootViewModel/BootRepository) plus §7.0's real NavHost (LedgerNavHost)
// replace what used to be a hardcoded `signedIn` boolean and a manual tab `when` here -- see
// BUILD-LOG's C9 entry for what this replaced and why.
//
// FragmentActivity, not ComponentActivity: androidx.biometric.BiometricPrompt (BiometricGate)
// needs one. FragmentActivity extends ComponentActivity, so this is a strict widening, not a
// behavior change for anything that used to depend on the narrower type.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var userPrefsRepository: UserPrefsRepository
    @Inject lateinit var syncScheduler: SyncScheduler

    private val bootViewModel: BootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // §7.1: "Single decision point, no UI beyond the system splash." Theme.App.Starting
        // (themes.xml) is the launch window's own background; this defers the first real frame
        // until BootViewModel.route resolves, the same technique the androidx.core.splashscreen
        // compat library uses internally (see themes.xml's own note on why that library itself
        // isn't used here) -- so the launch background stays on screen with no flicker and no
        // loading UI of our own underneath it.
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val ready = bootViewModel.route != null
                    if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return ready
                }
            },
        )

        // Trigger 1 (§5.5): app cold start. Harmless with no session -- SyncRepository.sync()
        // itself no-ops to UNAUTHORIZED when SessionStore has nothing to read. Trigger 2
        // (foregrounded) is NOT wired here: §5.5's own text makes it the caller's job to check
        // ">60s since last pull" first, which needs a last-pull timestamp nothing tracks yet --
        // a real gap, flagged rather than built as an every-foreground spam trigger instead.
        syncScheduler.onColdStart()

        // §7.13's "Hide amounts in the app switcher" -- the one security toggle enforced via
        // FLAG_SECURE end to end (independent of the biometric-to-open gate BiometricGate now
        // adds). FLAG_SECURE also blocks screenshots as a side effect, a reasonable bonus for a
        // finance app, not just a switcher-thumbnail concern.
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
            LedgerRoot(bootViewModel)
        }
    }
}

@Composable
private fun LedgerRoot(bootViewModel: BootViewModel) {
    val route = bootViewModel.route ?: return // splash screen is still covering this frame
    LedgerTheme {
        LedgerNavHost(bootRoute = route)
    }
}
