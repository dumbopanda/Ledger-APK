package com.dumbopanda.ledger.feature.settings

import com.dumbopanda.ledger.core.data.sync.SyncMode

sealed interface SettingsAction {
    data class SyncModeChanged(val mode: SyncMode) : SettingsAction
    data object SendNowTapped : SettingsAction
    data class ShowAmountsToggled(val show: Boolean) : SettingsAction
    data class RequireBiometricToggled(val require: Boolean) : SettingsAction

    // §7.16's sign-out guard.
    data object SignOutTapped : SettingsAction
    data object SendPendingThenSignOutTapped : SettingsAction // "Send them first" (default)
    data object SignOutConfirmed : SettingsAction // "Sign out anyway"
    data object SignOutGuardDismissed : SettingsAction

    // C11's own block -- present so the row exists, not wired to real deletion yet.
    data object DeleteAccountTapped : SettingsAction
}
