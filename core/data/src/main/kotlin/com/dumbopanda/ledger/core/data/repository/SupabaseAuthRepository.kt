package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.datastore.Session
import com.dumbopanda.ledger.core.datastore.SessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
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

    override suspend fun refreshSession(): Boolean = runCatching {
        supabase.auth.refreshCurrentSession()
        val session = requireNotNull(supabase.auth.currentSessionOrNull())
        sessionStore.write(
            Session(
                userId = session.user?.id ?: error("Refreshed session has no user id"),
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            ),
        )
    }.onFailure { e -> Log.e(TAG, "refreshSession failed", e) }.isSuccess

    override fun currentEmail(): String? = supabase.auth.currentSessionOrNull()?.user?.email

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        supabase.postgrest.rpc("delete_my_account", buildJsonObject { })
        Unit
    }.onFailure { e -> Log.e(TAG, "deleteAccount failed", e) }
}
