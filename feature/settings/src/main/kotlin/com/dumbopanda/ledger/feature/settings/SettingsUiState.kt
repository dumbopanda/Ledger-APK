package com.dumbopanda.ledger.feature.settings

import com.dumbopanda.ledger.core.data.sync.SyncMode

data class SettingsUiState(
    val syncMode: SyncMode,
    val pendingOpCount: Int,
    val deadLetterCount: Int,
    val showAmountsInAppSwitcher: Boolean,
    val requireBiometricToOpen: Boolean,
    val signedInEmail: String?,
    // §7.16: refused while pending ops exist, with an honest count.
    val showSignOutGuard: Boolean,
    val signingOut: Boolean,
)
