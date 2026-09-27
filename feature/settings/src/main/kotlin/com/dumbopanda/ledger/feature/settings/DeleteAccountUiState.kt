package com.dumbopanda.ledger.feature.settings

// §7.17's 7 steps, minus the two the caller (DeleteAccountScreen) drives with plain local
// dialog state (step 2's backup nudge, step 3's "type DELETE") -- this only models the parts
// that touch the network: re-authenticate, then delete.
sealed interface DeleteAccountUiState {
    data object Idle : DeleteAccountUiState
    data object Reauthenticating : DeleteAccountUiState
    data class ReauthFailed(val message: String) : DeleteAccountUiState

    // C13: an email-sign-in account has no Credential Manager account to re-prove -- step 4's
    // re-authentication becomes "prove you still control this inbox" instead, a fresh code sent
    // to the ALREADY-signed-in address (never one typed here) and verified before deletion
    // proceeds. Same shape as feature:auth's EmailSignInUiState.EnteringCode, kept separate
    // since this one carries no email field (it's fixed) and continues straight to delete().
    data class EmailReauth(
        val email: String,
        val code: String = "",
        val resendInS: Int = 0,
        val error: String? = null,
        val sending: Boolean = false,
        val verifying: Boolean = false,
    ) : DeleteAccountUiState

    data object Deleting : DeleteAccountUiState
    data class DeleteFailed(val message: String) : DeleteAccountUiState
    // Step 7's confirmation screen reads this directly rather than firing a one-shot event --
    // matches BootRoute/SignInUiState.Success's own "state IS the signal" shape.
    data object Deleted : DeleteAccountUiState
}
