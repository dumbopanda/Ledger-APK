package com.dumbopanda.ledger.core.data.repository

// C13: email sign-in by 6-digit code. Domain-level mirrors of core:network's
// EmailOtpSendOutcome/EmailOtpVerifyOutcome (SupabaseEmailOtpApi) rather than
// exposing those directly -- same boundary SyncRepository/SyncOutcome draws
// against SyncApi, so feature modules (which depend on core:data only, not
// core:network) never need that module on their own compile classpath.
sealed interface EmailCodeSendOutcome {
    data class Sent(val expiresInS: Int, val resendInS: Int) : EmailCodeSendOutcome

    // reason: "invalid_email" | "cooldown" | "too_many" | "email_unavailable" | "bad_request".
    data class Rejected(val reason: String, val retryAfterS: Int?) : EmailCodeSendOutcome
    data object ServerError : EmailCodeSendOutcome
}

sealed interface EmailCodeVerifyOutcome {
    data object Success : EmailCodeVerifyOutcome

    // reason: "wrong_code" | "no_code" | "expired" | "locked" | "bad_request".
    data class Rejected(val reason: String, val attemptsLeft: Int?) : EmailCodeVerifyOutcome
    data object ServerError : EmailCodeVerifyOutcome
}

interface AuthRepository {
    // rawNonce must be the same string whose SHA-256 hash was passed to
    // GetSignInWithGoogleOption's setNonce (§7.3) — Supabase verifies the
    // hash server-side to confirm this token was minted for this exact
    // sign-in attempt, not replayed from another.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String>

    // C13: the email-otp Edge Function's two actions. verifyEmailCode, on Success, has already
    // imported the returned session into the Supabase client and written SessionStore -- same
    // end state signInWithGoogleIdToken leaves, so callers don't separately handle a token.
    suspend fun sendEmailCode(email: String): EmailCodeSendOutcome
    suspend fun verifyEmailCode(email: String, code: String): EmailCodeVerifyOutcome

    suspend fun signOut()

    // §5.3 rule 5: on a push's 401, refresh once and retry once before
    // giving up to SessionExpired. Returns false if the refresh itself fails
    // (e.g. the refresh token is also invalid).
    suspend fun refreshSession(): Boolean

    // §7.13's Settings > Account: "Signed-in Google address." A synchronous read of whatever
    // the auth client currently holds in memory -- SessionStore (§7.3) deliberately stores
    // only userId/accessToken/refreshToken, not email, so this is the one place email is
    // available at all.
    fun currentEmail(): String?

    // C13: "google" | "email" | null (signed out, or an unrecognized provider). Read from the
    // session's own app_metadata.provider rather than tracked separately on sign-in, so it's
    // always in sync with whatever the ambient session actually is. Settings/delete-account use
    // this to pick which re-authentication flow applies -- Credential Manager for a Google
    // account, a fresh code for an email one.
    fun currentAuthProvider(): String?

    // C14: Google's own ID token claims (name/full_name), which Supabase's GoTrue stores on
    // `auth.users.raw_user_meta_data` automatically for every OIDC sign-in, no extra plumbing
    // needed on the sign-in call itself. Null for an email sign-in (nothing to read) or if
    // Google didn't supply a name. ProfileSetupScreen prefills its name field with this so a
    // Google user only has to confirm it, not type it.
    fun nameHintFromProvider(): String?

    // The name typed on the email Sign up form, held in memory only until ProfileSetup saves it
    // to `profiles` -- an email sign-in has no provider name for nameHintFromProvider to read.
    fun rememberSignUpName(name: String?)

    // §7.17 step 5: `delete_my_account()` (0007_account_deletion.sql) -- SECURITY DEFINER,
    // trusts whatever `auth.uid()` the caller's current JWT carries, with no server-side
    // re-authentication check of its own. Step 4's Credential Manager re-auth is therefore a
    // client-side UX gate only; this call itself just uses the ALREADY-ambient session, not
    // whatever came back from re-auth (see AccountDeletionRepository's own doc for why re-auth
    // never touches SessionStore or the Supabase client's session state).
    suspend fun deleteAccount(): Result<Unit>
}
