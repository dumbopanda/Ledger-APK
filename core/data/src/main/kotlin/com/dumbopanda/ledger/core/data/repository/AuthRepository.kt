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
}
