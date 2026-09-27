package com.dumbopanda.ledger.feature.auth

// C13's email sign-in: two steps in one flow, "enter email" then "enter code" -- same shape as
// Onboarding's own single-screen, internal-pane pattern rather than two separate nav routes,
// since there's no reason to let the back stack separate them. [name] is only asked for in
// Sign up mode; it rides along so "Change" on the code screen brings it back untouched.
sealed interface EmailSignInUiState {
    data class EnteringEmail(
        val email: String = "",
        val error: String? = null,
        val name: String = "",
        val nameError: String? = null,
    ) : EmailSignInUiState

    data class SendingCode(val email: String, val name: String = "") : EmailSignInUiState

    // resendInS counts down client-side from the server's own resend_in_s (0013's 2-minute
    // cooldown) -- it's a UX nicety (disables Resend until it's actually likely to work),
    // never itself a security boundary; the server enforces the real cooldown regardless.
    data class EnteringCode(
        val email: String,
        val code: String = "",
        val resendInS: Int = 0,
        val error: String? = null,
        val verifying: Boolean = false,
        // A resend in flight -- stays on the code screen (the old SendingCode bounce would flash
        // the email screen back for the length of the network call).
        val resending: Boolean = false,
        val name: String = "",
    ) : EmailSignInUiState

    data object Success : EmailSignInUiState
}
