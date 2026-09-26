package com.dumbopanda.ledger.feature.settings

import com.dumbopanda.ledger.core.data.sync.SyncMode

data class SettingsUiState(
    val syncMode: SyncMode,
    val pendingOpCount: Int,
    val deadLetterCount: Int,
    // "Waiting to be sent" -- "Nothing waiting" / "1 entry · by 02:15" / "3 entries · offline".
    val queueSummary: String,
    // The Send button's own label ("Send" / "Offline") and whether it can be tapped.
    val sendLabel: String,
    val sendEnabled: Boolean,
    // The line under Batched / Instant / Manual.
    val syncModeHint: String,
    // "5 entries · never backed up" / "5 entries · 27 Sept, 22:04".
    val backupStatus: String,
    val accountCount: Int,
    val showAmountsInAppSwitcher: Boolean,
    val requireBiometricToOpen: Boolean,
    val signedInEmail: String?,
    // §7.16: refused while pending ops exist, with an honest count.
    val showSignOutGuard: Boolean,
    val signingOut: Boolean,
    val exporting: Boolean,
    val importing: Boolean,
)
