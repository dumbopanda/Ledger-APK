package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.datastore.Session
import com.dumbopanda.ledger.core.datastore.SessionStore
import com.dumbopanda.ledger.core.network.auth.EmailOtpApi
import com.dumbopanda.ledger.core.network.auth.EmailOtpSendOutcome
import com.dumbopanda.ledger.core.network.auth.EmailOtpVerifyOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionStore: SessionStore,
    private val emailOtpApi: EmailOtpApi,
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

    override suspend fun sendEmailCode(email: String): EmailCodeSendOutcome =
        when (val outcome = emailOtpApi.sendCode(email)) {
            is EmailOtpSendOutcome.Sent -> EmailCodeSendOutcome.Sent(outcome.expiresInS, outcome.resendInS)
            is EmailOtpSendOutcome.Rejected -> EmailCodeSendOutcome.Rejected(outcome.reason, outcome.retryAfterS)
            EmailOtpSendOutcome.ServerError -> EmailCodeSendOutcome.ServerError
        }

    override suspend fun verifyEmailCode(email: String, code: String): EmailCodeVerifyOutcome =
        when (val outcome = emailOtpApi.verifyCode(email, code)) {
            is EmailOtpVerifyOutcome.Success -> {
                // The Edge Function already minted this via Supabase Auth server-side (admin
                // createUser + generateLink + verifyOtp) -- this adopts it as the client's own
                // ambient session rather than fabricating one with no server round trip.
                //
                // Deliberately NOT auth.importAuthToken(accessToken, refreshToken): that
                // convenience wrapper hardcodes expiresIn=0L on the UserSession it builds
                // (supabase-kt 3.8.0's Auth.kt), which makes importSession() decide the session
                // is already past its refresh threshold and hand off to recreateSessionJob --
                // a fire-and-forget coroutine on Auth's own authScope that this call never
                // awaits (it only joins when initializing=true, which importAuthToken's call
                // path never sets). The result, confirmed live on an emulator: importAuthToken
                // returns normally, but currentSessionOrNull() right after it is still null --
                // the session gets populated moments later, off this coroutine entirely.
                // Building the UserSession here with the REAL expires_in the Edge Function
                // returned avoids that branch altogether: importSession sees a session that
                // isn't already due for a refresh and sets it synchronously instead.
                runCatching {
                    val user = supabase.auth.retrieveUser(outcome.accessToken)
                    val session = UserSession(
                        accessToken = outcome.accessToken,
                        refreshToken = outcome.refreshToken,
                        expiresIn = outcome.expiresIn,
                        tokenType = "bearer",
                        user = user,
                    )
                    supabase.auth.importSession(session, autoRefresh = true)
                    sessionStore.write(Session(userId = user.id, accessToken = outcome.accessToken, refreshToken = outcome.refreshToken))
                }.fold(
                    { EmailCodeVerifyOutcome.Success },
                    { e -> Log.e(TAG, "importing session after verifyEmailCode failed", e); EmailCodeVerifyOutcome.ServerError },
                )
            }
            is EmailOtpVerifyOutcome.Rejected -> EmailCodeVerifyOutcome.Rejected(outcome.reason, outcome.attemptsLeft)
            EmailOtpVerifyOutcome.ServerError -> EmailCodeVerifyOutcome.ServerError
        }

    private companion object {
        const val TAG = "SupabaseAuthRepository"
    }

    override suspend fun signOut() {
        signUpName = null
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

    override fun currentAuthProvider(): String? =
        supabase.auth.currentSessionOrNull()?.user?.appMetadata?.get("provider")?.jsonPrimitive?.contentOrNull

    @Volatile private var signUpName: String? = null

    override fun rememberSignUpName(name: String?) {
        signUpName = name?.trim()?.takeIf { it.isNotEmpty() }
    }

    override fun nameHintFromProvider(): String? {
        val metadata = supabase.auth.currentSessionOrNull()?.user?.userMetadata
        // Google's own ID token uses "name"; some providers use "full_name" instead -- try both
        // rather than assuming which one this session's provider populated.
        val fromProvider = (metadata?.get("full_name") ?: metadata?.get("name"))
            ?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        return fromProvider ?: signUpName
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        supabase.postgrest.rpc("delete_my_account", buildJsonObject { })
        Unit
    }.onFailure { e -> Log.e(TAG, "deleteAccount failed", e) }
}
