package com.dumbopanda.ledger.core.data.repository

interface AuthRepository {
    // rawNonce must be the same string whose SHA-256 hash was passed to
    // GetSignInWithGoogleOption's setNonce (§7.3) — Supabase verifies the
    // hash server-side to confirm this token was minted for this exact
    // sign-in attempt, not replayed from another.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String>

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

    // §7.17 step 5: `delete_my_account()` (0007_account_deletion.sql) -- SECURITY DEFINER,
    // trusts whatever `auth.uid()` the caller's current JWT carries, with no server-side
    // re-authentication check of its own. Step 4's Credential Manager re-auth is therefore a
    // client-side UX gate only; this call itself just uses the ALREADY-ambient session, not
    // whatever came back from re-auth (see AccountDeletionRepository's own doc for why re-auth
    // never touches SessionStore or the Supabase client's session state).
    suspend fun deleteAccount(): Result<Unit>
}
