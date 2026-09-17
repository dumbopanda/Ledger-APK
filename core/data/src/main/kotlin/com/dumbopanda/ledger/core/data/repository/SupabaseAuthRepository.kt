package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.datastore.Session
import com.dumbopanda.ledger.core.datastore.SessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionStore: SessionStore,
) : AuthRepository {
    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String> =
        runCatching {
            Log.d(TAG, "Calling supabase.auth.signInWith(IDToken)...")
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = rawNonce
            }
            Log.d(TAG, "signInWith(IDToken) returned without throwing")

            val session = requireNotNull(supabase.auth.currentSessionOrNull()) {
                "signInWith(IDToken) returned without raising, but no session is present"
            }
            Log.d(TAG, "Session present, user id=${session.user?.id}")

            sessionStore.write(
                Session(
                    userId = session.user?.id ?: error("Session has no user id"),
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                ),
            )

            session.user!!.id
        }.onFailure { e -> Log.e(TAG, "signInWithGoogleIdToken failed", e) }

    private companion object {
        const val TAG = "SupabaseAuthRepository"
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
        sessionStore.clear()
    }
}
