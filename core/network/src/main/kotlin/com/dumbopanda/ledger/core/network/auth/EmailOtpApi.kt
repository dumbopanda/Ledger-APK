package com.dumbopanda.ledger.core.network.auth

// C13: email sign-in by 6-digit code, alongside Google. Every rule (5-minute
// expiry, 60s/hourly/daily caps, 5-guess lockout, consume-on-success) is
// enforced server-side by otp_issue()/otp_check() (0011_email_otp.sql) via
// the email-otp Edge Function -- this is purely the transport, one call in
// and one typed outcome out.
sealed interface EmailOtpSendOutcome {
    data class Sent(val expiresInS: Int, val resendInS: Int) : EmailOtpSendOutcome

    // reason: "invalid_email" | "cooldown" | "too_many" | "email_unavailable" | "bad_request".
    // retryAfterS is set for "cooldown"/"too_many" only.
    data class Rejected(val reason: String, val retryAfterS: Int?) : EmailOtpSendOutcome
    data object ServerError : EmailOtpSendOutcome
}

sealed interface EmailOtpVerifyOutcome {
    // expiresIn matters beyond just carrying data: importSession (SupabaseAuthRepository) needs
    // a real, non-zero expiry to take its synchronous "session is fresh enough" path rather than
    // its async "needs a background refresh first" one -- see that call site's own comment.
    data class Success(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: String) : EmailOtpVerifyOutcome

    // reason: "wrong_code" | "no_code" | "expired" | "locked" | "bad_request".
    // attemptsLeft is set for "wrong_code" only.
    data class Rejected(val reason: String, val attemptsLeft: Int?) : EmailOtpVerifyOutcome
    data object ServerError : EmailOtpVerifyOutcome
}

interface EmailOtpApi {
    suspend fun sendCode(email: String): EmailOtpSendOutcome
    suspend fun verifyCode(email: String, code: String): EmailOtpVerifyOutcome
}
