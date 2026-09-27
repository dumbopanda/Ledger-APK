package com.dumbopanda.ledger.feature.settings

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.backup.BackupRepository
import com.dumbopanda.ledger.core.data.backup.ExportOutcome
import com.dumbopanda.ledger.core.data.repository.AccountDeletionOutcome
import com.dumbopanda.ledger.core.data.repository.AccountDeletionRepository
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.data.repository.EmailCodeSendOutcome
import com.dumbopanda.ledger.core.data.repository.EmailCodeVerifyOutcome
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

/**
 * §7.17's 7 steps, plus C13's second re-authentication path. Step 4's Credential Manager
 * re-authentication (Google accounts) is deliberately NOT a call to
 * [AuthRepository.signInWithGoogleIdToken] -- that would exchange the credential with Supabase
 * and overwrite the ambient session with whatever account Credential Manager returned, which is
 * exactly wrong if the person picks a DIFFERENT Google account than the one being deleted (see
 * [AccountDeletionRepository]'s own doc). Instead this decodes the returned ID token's own
 * `email` claim locally -- no network round trip, no session mutation -- and compares it
 * against [AuthRepository.currentEmail]. This is a client-side UX gate, matching exactly how
 * much identity-proof `delete_my_account()` itself requires (none beyond the ambient JWT).
 *
 * An email-sign-in account has no Google credential to re-prove, so [reauthenticateAndDelete]
 * branches on [AuthRepository.currentAuthProvider]: for "email" it sends a fresh code to the
 * ALREADY-signed-in address (never one typed here -- there's nothing to type, it's fixed) via
 * [DeleteAccountUiState.EmailReauth], and [AuthRepository.verifyEmailCode] succeeding on it IS
 * the re-auth proof, whereupon [delete] runs the same as the Google path. That call also
 * refreshes the ambient session's tokens as a side effect (same account, so harmless) --
 * [AccountDeletionRepository] doesn't care either way, it uses whatever session is ambient.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountDeletionRepository: AccountDeletionRepository,
    private val backupRepository: BackupRepository,
    userPrefsRepository: UserPrefsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Idle)
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    val lastBackupAt: StateFlow<Instant?> = userPrefsRepository.lastBackupAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var hasExportedThisSession: Boolean = false
        private set

    val signedInEmail: String? get() = authRepository.currentEmail()

    private var reauthCountdownJob: Job? = null

    fun export(uri: android.net.Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = when (val outcome = backupRepository.exportTo(uri)) {
                is ExportOutcome.Success -> {
                    hasExportedThisSession = true
                    "File saved: ${outcome.accountCount} accounts, ${outcome.entryCount} entries"
                }
                ExportOutcome.NotSignedIn -> "Sign in before saving a file."
                ExportOutcome.WriteError -> "Couldn't write the backup file"
            }
            onResult(message)
        }
    }

    // C13: routes to whichever re-auth this account actually has. An email-sign-in account has
    // no Google credential to re-prove at all -- Credential Manager would just open a picker of
    // whatever Google accounts happen to be on the device, which proves nothing about THIS
    // account's identity.
    fun reauthenticateAndDelete(context: Context) {
        if (_uiState.value == DeleteAccountUiState.Reauthenticating || _uiState.value == DeleteAccountUiState.Deleting) return

        if (authRepository.currentAuthProvider() == "email") {
            val email = authRepository.currentEmail()
            if (email == null) {
                _uiState.value = DeleteAccountUiState.ReauthFailed("You're not signed in.")
            } else {
                sendReauthCode(email)
            }
            return
        }

        reauthenticateWithGoogleAndDelete(context)
    }

    fun onReauthCodeChanged(code: String) {
        val state = _uiState.value as? DeleteAccountUiState.EmailReauth ?: return
        _uiState.value = state.copy(code = code.filter { it.isDigit() }.take(6), error = null)
    }

    fun resendReauthCode() {
        val state = _uiState.value as? DeleteAccountUiState.EmailReauth ?: return
        if (state.resendInS > 0 || state.sending) return
        sendReauthCode(state.email)
    }

    fun verifyReauthCodeAndDelete() {
        val state = _uiState.value as? DeleteAccountUiState.EmailReauth ?: return
        if (state.code.length != 6 || state.verifying) return

        _uiState.value = state.copy(verifying = true, error = null)
        viewModelScope.launch {
            when (val outcome = authRepository.verifyEmailCode(state.email, state.code)) {
                EmailCodeVerifyOutcome.Success -> {
                    reauthCountdownJob?.cancel()
                    delete()
                }
                is EmailCodeVerifyOutcome.Rejected -> _uiState.value =
                    state.copy(code = "", verifying = false, error = emailReauthRejectionMessage(outcome))
                EmailCodeVerifyOutcome.ServerError -> _uiState.value =
                    state.copy(verifying = false, error = "Couldn't reach the server. Try again.")
            }
        }
    }

    private fun sendReauthCode(email: String) {
        val previous = _uiState.value as? DeleteAccountUiState.EmailReauth
        _uiState.value = DeleteAccountUiState.EmailReauth(email, code = previous?.code ?: "", sending = true)
        viewModelScope.launch {
            when (val outcome = authRepository.sendEmailCode(email)) {
                is EmailCodeSendOutcome.Sent -> {
                    _uiState.value = DeleteAccountUiState.EmailReauth(email, resendInS = outcome.resendInS)
                    startReauthCountdown(outcome.resendInS)
                }
                is EmailCodeSendOutcome.Rejected -> _uiState.value =
                    DeleteAccountUiState.EmailReauth(email, error = sendRejectionMessage(outcome))
                EmailCodeSendOutcome.ServerError -> _uiState.value =
                    DeleteAccountUiState.EmailReauth(email, error = "Couldn't reach the server. Try again.")
            }
        }
    }

    private fun startReauthCountdown(fromSeconds: Int) {
        reauthCountdownJob?.cancel()
        if (fromSeconds <= 0) return
        reauthCountdownJob = viewModelScope.launch {
            var remaining = fromSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.update { state -> if (state is DeleteAccountUiState.EmailReauth) state.copy(resendInS = remaining) else state }
            }
        }
    }

    private fun sendRejectionMessage(rejected: EmailCodeSendOutcome.Rejected): String = when (rejected.reason) {
        "cooldown" -> "Wait ${rejected.retryAfterS ?: 120}s before requesting another code"
        "too_many" -> "Too many codes requested. Try again in ${(rejected.retryAfterS ?: 3600) / 60} min"
        "email_unavailable" -> "Couldn't send the email right now. Try again in a moment."
        else -> "Couldn't send a code. Try again."
    }

    private fun emailReauthRejectionMessage(rejected: EmailCodeVerifyOutcome.Rejected): String = when (rejected.reason) {
        "wrong_code" -> {
            val left = rejected.attemptsLeft
            if (left != null) "That’s not it. $left ${if (left == 1) "try" else "tries"} left." else "Wrong code"
        }
        "expired" -> "That code has expired. Tap Resend for a new one."
        "locked" -> "Too many wrong tries, so that code is locked. Tap Resend for a new one."
        "no_code" -> "That code no longer works. Tap Resend for a new one."
        else -> "Couldn't verify that code. Try again."
    }

    private fun reauthenticateWithGoogleAndDelete(context: Context) {
        _uiState.value = DeleteAccountUiState.Reauthenticating

        viewModelScope.launch {
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = sha256(rawNonce)
            val option = GetSignInWithGoogleOption.Builder(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

            try {
                val result = CredentialManager.create(context).getCredential(context, request)
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                val reauthenticatedEmail = decodeJwtEmail(idToken)
                val currentEmail = authRepository.currentEmail()

                if (reauthenticatedEmail == null) {
                    _uiState.value = DeleteAccountUiState.ReauthFailed("Couldn't verify that account. Try again.")
                } else if (currentEmail != null && !reauthenticatedEmail.equals(currentEmail, ignoreCase = true)) {
                    _uiState.value = DeleteAccountUiState.ReauthFailed("That's a different Google account than the one signed in.")
                } else {
                    delete()
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = DeleteAccountUiState.Idle
            } catch (e: NoCredentialException) {
                _uiState.value = DeleteAccountUiState.ReauthFailed("No Google account on this device.")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException during re-auth", e)
                _uiState.value = DeleteAccountUiState.ReauthFailed("Couldn't verify that account. Try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception during re-auth", e)
                _uiState.value = DeleteAccountUiState.ReauthFailed("Couldn't verify that account. Try again.")
            }
        }
    }

    private suspend fun delete() {
        _uiState.value = DeleteAccountUiState.Deleting
        when (val outcome = accountDeletionRepository.deleteAccountAndWipeLocalData()) {
            AccountDeletionOutcome.Success -> _uiState.value = DeleteAccountUiState.Deleted
            AccountDeletionOutcome.NotSignedIn -> _uiState.value = DeleteAccountUiState.DeleteFailed("You're not signed in.")
            is AccountDeletionOutcome.ServerRejected -> _uiState.value = DeleteAccountUiState.DeleteFailed(outcome.message)
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onCleared() {
        reauthCountdownJob?.cancel()
    }

    private companion object {
        const val TAG = "DeleteAccountViewModel"
    }
}

// internal, not private -- DecodeJwtEmailTest exercises this directly rather than needing a
// real Credential Manager round trip. No signature verification: this is a UX-gate comparison
// only, matching exactly how much identity proof delete_my_account() itself requires server
// side (none beyond the ambient JWT already used to call it).
internal fun decodeJwtEmail(idToken: String): String? = runCatching {
    val payload = idToken.split(".")[1]
    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
    val decoded = String(Base64.getUrlDecoder().decode(padded))
    Json.parseToJsonElement(decoded).jsonObject["email"]?.jsonPrimitive?.content
}.getOrNull()
