package com.dumbopanda.ledger.feature.settings

// §7.17's 7 steps, minus the two the caller (DeleteAccountScreen) drives with plain local
// dialog state (step 2's backup nudge, step 3's "type DELETE") -- this only models the parts
// that touch the network: re-authenticate, then delete.
sealed interface DeleteAccountUiState {
    data object Idle : DeleteAccountUiState
    data object Reauthenticating : DeleteAccountUiState
    data class ReauthFailed(val message: String) : DeleteAccountUiState
    data object Deleting : DeleteAccountUiState
    data class DeleteFailed(val message: String) : DeleteAccountUiState
    // Step 7's confirmation screen reads this directly rather than firing a one-shot event --
    // matches BootRoute/SignInUiState.Success's own "state IS the signal" shape.
    data object Deleted : DeleteAccountUiState
}
