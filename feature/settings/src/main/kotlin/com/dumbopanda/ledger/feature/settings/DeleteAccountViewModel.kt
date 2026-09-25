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
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
 * §7.17's 7 steps. Step 4's Credential Manager re-authentication is deliberately NOT a call to
 * [AuthRepository.signInWithGoogleIdToken] -- that would exchange the credential with Supabase
 * and overwrite the ambient session with whatever account Credential Manager returned, which is
 * exactly wrong if the person picks a DIFFERENT Google account than the one being deleted (see
 * [AccountDeletionRepository]'s own doc). Instead this decodes the returned ID token's own
 * `email` claim locally -- no network round trip, no session mutation -- and compares it
 * against [AuthRepository.currentEmail]. This is a client-side UX gate, matching exactly how
 * much identity-proof `delete_my_account()` itself requires (none beyond the ambient JWT).
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

    fun export(uri: android.net.Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = when (val outcome = backupRepository.exportTo(uri)) {
                is ExportOutcome.Success -> {
                    hasExportedThisSession = true
                    "Backup saved -- ${outcome.accountCount} accounts, ${outcome.entryCount} entries"
                }
                ExportOutcome.NotSignedIn -> "Sign in before backing up"
                ExportOutcome.WriteError -> "Couldn't write the backup file"
            }
            onResult(message)
        }
    }

    fun reauthenticateAndDelete(context: Context) {
        if (_uiState.value == DeleteAccountUiState.Reauthenticating || _uiState.value == DeleteAccountUiState.Deleting) return
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
