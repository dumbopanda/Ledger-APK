package com.dumbopanda.ledger.feature.settings

import android.net.Uri
import com.dumbopanda.ledger.core.data.sync.SyncMode

sealed interface SettingsAction {
    data class SyncModeChanged(val mode: SyncMode) : SettingsAction
    data object SendNowTapped : SettingsAction
    data class ShowAmountsToggled(val show: Boolean) : SettingsAction
    data class RequireBiometricToggled(val require: Boolean) : SettingsAction

    // §7.14 -- the Composable already picked the Uri via SAF (ActivityResultContracts), this
    // ViewModel never touches the picker itself.
    data class ExportRequested(val uri: Uri) : SettingsAction
    data class ImportRequested(val uri: Uri) : SettingsAction
    data object NudgeSnoozed : SettingsAction

    // §7.16's sign-out guard.
    data object SignOutTapped : SettingsAction
    data object SendPendingThenSignOutTapped : SettingsAction // "Send them first" (default)
    data object SignOutConfirmed : SettingsAction // "Sign out anyway"
    data object SignOutGuardDismissed : SettingsAction

    // C11's own block -- present so the row exists, not wired to real deletion yet.
    data object DeleteAccountTapped : SettingsAction
}
