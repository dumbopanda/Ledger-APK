package com.dumbopanda.ledger.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton

private const val PANE_COUNT = 3
private const val CONSENT_PANE_INDEX = 2

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(onAgreeAndContinue = { viewModel.agreeAndContinue(onFinished) })
}

@Composable
internal fun OnboardingScreen(onAgreeAndContinue: () -> Unit) {
    var pane by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                when (pane) {
                    0 -> WhatItDoesPane()
                    1 -> DependencyPane()
                    else -> ConsentPane()
                }
            }

            PaneIndicator(current = pane, count = PANE_COUNT)
            Spacer(Modifier.height(16.dp))

            when (pane) {
                CONSENT_PANE_INDEX -> {
                    LedgerPillButton(
                        text = "Agree and continue",
                        onClick = onAgreeAndContinue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // §7.2: "Skippable: panes 1–2 yes" -- either pane can jump straight to
                        // the consent pane, which is never itself skipped.
                        TextButton(onClick = { pane = CONSENT_PANE_INDEX }) { Text("Skip") }
                        LedgerPillButton(text = "Next", onClick = { pane += 1 })
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatItDoesPane() {
    Text("Track your money", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text(
        "One screen. Track money in and out across your accounts, offline, on your phone.",
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun DependencyPane() {
    Text("Sign-in is Google only", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text(
        "If you lose access to that Google account, you lose the ledger -- there is no " +
            "password recovery for it. The way out is your own backup file, not us.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(20.dp))
    // Export needs a signed-in, per-user database that doesn't exist yet at this point in
    // the boot flow (Onboarding runs before Sign in, §7.1 row 1) -- shown and explained here
    // per §7.2's own requirement ("the export action is offered here, not buried in
    // settings"), disabled rather than wired to a no-op, same pattern C7 used for Backup
    // before C8 existed to build it.
    LedgerPillButton(text = "Export a backup", onClick = {}, enabled = false)
    Spacer(Modifier.height(8.dp))
    Text(
        "Nothing to export yet -- you'll see this again in Settings once you're signed in " +
            "and have entries. It takes four seconds.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConsentPane() {
    Text("Before you continue", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text(
        "We collect your email address and Google account ID, and nothing else -- no phone " +
            "number, no name beyond what Google gives us, no device fingerprint.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "It's used only to run this ledger: sign you in and store your entries and " +
            "accounts, kept in Supabase (ap-south-1). It's never sold, shared, or used for ads.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Delete it anytime from Settings -> Delete account -- immediate and irreversible.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(16.dp))
    // Phase 9's own gate is "both policy URLs resolve" -- neither exists yet (no domain has
    // been registered in this project as of C9), so this stays plain, un-clickable text
    // rather than a Uri.parse() to a page that isn't live.
    LedgerBanner(
        text = "Privacy policy: link added once published.",
        tone = LedgerBannerTone.Info,
    )
}

@Composable
private fun PaneIndicator(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(count) { index ->
            val color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun OnboardingScreenPreview() {
    LedgerTheme {
        OnboardingScreen(onAgreeAndContinue = {})
    }
}
