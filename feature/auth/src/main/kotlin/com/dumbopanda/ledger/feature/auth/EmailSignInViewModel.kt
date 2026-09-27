package com.dumbopanda.ledger.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.data.repository.EmailCodeSendOutcome
import com.dumbopanda.ledger.core.data.repository.EmailCodeVerifyOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@HiltViewModel
class EmailSignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EmailSignInUiState>(EmailSignInUiState.EnteringEmail())
    val uiState: StateFlow<EmailSignInUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onEmailChanged(email: String) {
        val state = _uiState.value
        if (state is EmailSignInUiState.EnteringEmail) _uiState.value = state.copy(email = email, error = null)
    }

    fun onNameChanged(name: String) {
        val state = _uiState.value
        if (state is EmailSignInUiState.EnteringEmail) _uiState.value = state.copy(name = name.take(60), nameError = null)
    }

    fun onCodeChanged(code: String) {
        val state = _uiState.value
        // The server only ever issues 6 digits (index.ts's randomCode()) -- filtering keeps
        // the field itself from ever showing something that couldn't possibly be right.
        if (state is EmailSignInUiState.EnteringCode) {
            val digits = code.filter { it.isDigit() }.take(6)
            _uiState.value = state.copy(code = digits, error = null)
            // The sixth digit (typed or pasted) submits on its own -- there's nothing else to do.
            if (digits.length == 6 && state.code.length < 6) verifyCode()
        }
    }

    // "Change" / back from the code screen: same address, ready to edit.
    fun changeEmail() {
        val state = _uiState.value as? EmailSignInUiState.EnteringCode ?: return
        countdownJob?.cancel()
        _uiState.value = EmailSignInUiState.EnteringEmail(state.email, name = state.name)
    }

    // signUp: the Sign up tab also asks for a name, which is required there and ignored on
    // Sign in (the first verified code creates the account either way).
    fun sendCode(signUp: Boolean) {
        val state = _uiState.value as? EmailSignInUiState.EnteringEmail ?: return
        val email = state.email.trim()
        val name = if (signUp) state.name.trim() else ""
        val nameError = if (signUp && name.isEmpty()) "Tell us what to call you" else null
        val emailError = if (!EMAIL_PATTERN.matches(email)) "Enter a valid email address" else null
        if (nameError != null || emailError != null) {
            _uiState.value = state.copy(error = emailError, nameError = nameError)
            return
        }

        _uiState.value = EmailSignInUiState.SendingCode(email, name)
        viewModelScope.launch {
            when (val outcome = authRepository.sendEmailCode(email)) {
                is EmailCodeSendOutcome.Sent -> enterCodeState(email, outcome.resendInS, name = name)
                is EmailCodeSendOutcome.Rejected -> _uiState.value =
                    EmailSignInUiState.EnteringEmail(email, sendRejectionMessage(outcome), name = state.name)
                EmailCodeSendOutcome.ServerError -> _uiState.value =
                    EmailSignInUiState.EnteringEmail(email, "Couldn't reach the server. Try again.", name = state.name)
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value as? EmailSignInUiState.EnteringCode ?: return
        if (state.resendInS > 0 || state.resending) return
        val email = state.email
        _uiState.value = state.copy(resending = true, error = null)
        viewModelScope.launch {
            when (val outcome = authRepository.sendEmailCode(email)) {
                is EmailCodeSendOutcome.Sent -> enterCodeState(email, outcome.resendInS, name = state.name)
                is EmailCodeSendOutcome.Rejected ->
                    enterCodeState(email, resendInS = outcome.retryAfterS ?: 0, error = sendRejectionMessage(outcome), name = state.name)
                EmailCodeSendOutcome.ServerError ->
                    enterCodeState(email, resendInS = 0, error = "Couldn't reach the server. Try again.", name = state.name)
            }
        }
    }

    fun verifyCode() {
        val state = _uiState.value as? EmailSignInUiState.EnteringCode ?: return
        if (state.code.length != 6 || state.verifying) return

        _uiState.value = state.copy(verifying = true, error = null)
        viewModelScope.launch {
            when (val outcome = authRepository.verifyEmailCode(state.email, state.code)) {
                EmailCodeVerifyOutcome.Success -> {
                    countdownJob?.cancel()
                    authRepository.rememberSignUpName(state.name)
                    _uiState.value = EmailSignInUiState.Success
                }
                is EmailCodeVerifyOutcome.Rejected -> _uiState.value =
                    state.copy(code = "", verifying = false, error = verifyRejectionMessage(outcome))
                EmailCodeVerifyOutcome.ServerError -> _uiState.value =
                    state.copy(verifying = false, error = "Couldn't reach the server. Try again.")
            }
        }
    }

    private fun enterCodeState(email: String, resendInS: Int, error: String? = null, name: String = "") {
        _uiState.value = EmailSignInUiState.EnteringCode(email, resendInS = resendInS, error = error, name = name)
        startCountdown(resendInS)
    }

    private fun startCountdown(fromSeconds: Int) {
        countdownJob?.cancel()
        if (fromSeconds <= 0) return
        countdownJob = viewModelScope.launch {
            var remaining = fromSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.update { state ->
                    if (state is EmailSignInUiState.EnteringCode) state.copy(resendInS = remaining) else state
                }
            }
        }
    }

    private fun sendRejectionMessage(rejected: EmailCodeSendOutcome.Rejected): String = when (rejected.reason) {
        "invalid_email" -> "Enter a valid email address"
        "cooldown" -> "Wait ${rejected.retryAfterS ?: 120}s before requesting another code"
        "too_many" -> "Too many codes requested. Try again in ${(rejected.retryAfterS ?: 3600) / 60} min"
        "email_unavailable" -> "Couldn't send the email right now. Try again in a moment."
        else -> "Couldn't send a code. Try again."
    }

    private fun verifyRejectionMessage(rejected: EmailCodeVerifyOutcome.Rejected): String = when (rejected.reason) {
        "wrong_code" -> {
            val left = rejected.attemptsLeft
            if (left != null) "That’s not it. $left ${if (left == 1) "try" else "tries"} left." else "Wrong code"
        }
        "expired" -> "That code has expired. Tap Resend for a new one."
        "locked" -> "Too many wrong tries, so that code is locked. Tap Resend for a new one."
        "no_code" -> "That code no longer works. Tap Resend for a new one."
        else -> "Couldn't verify that code. Try again."
    }

    override fun onCleared() {
        countdownJob?.cancel()
    }
}
