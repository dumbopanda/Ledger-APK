package com.dumbopanda.ledger.feature.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(context: Context) {
        if (_uiState.value == SignInUiState.InProgress) return
        _uiState.value = SignInUiState.InProgress

        viewModelScope.launch {
            // Google verifies the SHA-256 hash it was given matches this raw
            // value at token-mint time; Supabase re-verifies the same hash
            // server-side against the token's nonce claim (§7.3) — the raw
            // value never goes anywhere but here and the signInWith call below.
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = sha256(rawNonce)

            val option = GetSignInWithGoogleOption.Builder(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

            try {
                Log.d(TAG, "Requesting credential...")
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                Log.d(TAG, "Credential received: type=${result.credential.type}")

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                Log.d(TAG, "Parsed GoogleIdTokenCredential, idToken length=${googleIdTokenCredential.idToken.length}")

                val authResult = authRepository.signInWithGoogleIdToken(googleIdTokenCredential.idToken, rawNonce)
                authResult
                    .onSuccess { userId -> Log.d(TAG, "signInWithGoogleIdToken SUCCESS, userId=$userId") }
                    .onFailure { error -> Log.e(TAG, "signInWithGoogleIdToken FAILED", error) }

                authResult
                    .onSuccess { _uiState.value = SignInUiState.Success }
                    .onFailure { error ->
                        // A transport failure reaching Supabase (§7.3's "Couldn't reach
                        // the server") is a different state from the server actually
                        // rejecting the token — only the latter should ever get logged.
                        _uiState.value = if (error is java.io.IOException) {
                            SignInUiState.NetworkFailure("Couldn't reach the server. Try again.")
                        } else {
                            SignInUiState.TokenRejected
                        }
                    }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Cancelled by user")
                // Cancelling is not an error (§7.3) — silent return to Idle.
                _uiState.value = SignInUiState.Idle
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No credential on device", e)
                _uiState.value = SignInUiState.NoGoogleAccountOnDevice
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException", e)
                _uiState.value = SignInUiState.NetworkFailure(e.message ?: "Couldn't reach the server.")
            } catch (e: Exception) {
                // Anything else (e.g. GoogleIdTokenParsingException, which isn't a
                // GetCredentialException subtype and would otherwise crash the
                // coroutine silently) — caught here now, was NOT caught before,
                // and is exactly the kind of gap that left the previous run
                // unexplainable.
                Log.e(TAG, "Unexpected exception in sign-in flow", e)
                _uiState.value = SignInUiState.TokenRejected
            }
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "SignInViewModel"
    }
}
