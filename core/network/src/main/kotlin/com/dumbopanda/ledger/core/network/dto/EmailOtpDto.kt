package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Matches the email-otp Edge Function's request/response shapes exactly
// (supabase/functions/email-otp/index.ts). Every rule -- expiry, cooldown,
// caps, lockout -- lives server-side (0011_email_otp.sql); these DTOs only
// carry its answer back for the UI to show.

@Serializable
data class EmailOtpRequestDto(val action: String, val email: String, val code: String? = null)

@Serializable
data class EmailOtpSendOkDto(
    @SerialName("expires_in_s") val expiresInS: Int,
    @SerialName("resend_in_s") val resendInS: Int,
)

@Serializable
data class EmailOtpVerifyOkDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: EmailOtpUserDto,
)

@Serializable
data class EmailOtpUserDto(val id: String, val email: String? = null)

// The one error shape every non-2xx response uses -- "reason" from
// otp_issue()/otp_check() surfaces here as "error" (send()/verify() in
// index.ts rename it), plus whichever of the two optional hints applies.
@Serializable
data class EmailOtpErrorDto(
    val error: String,
    @SerialName("retry_after_s") val retryAfterS: Int? = null,
    @SerialName("attempts_left") val attemptsLeft: Int? = null,
)
