package com.dumbopanda.ledger.core.data.repository

interface AuthRepository {
    // rawNonce must be the same string whose SHA-256 hash was passed to
    // GetSignInWithGoogleOption's setNonce (§7.3) — Supabase verifies the
    // hash server-side to confirm this token was minted for this exact
    // sign-in attempt, not replayed from another.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String>

    suspend fun signOut()
}
