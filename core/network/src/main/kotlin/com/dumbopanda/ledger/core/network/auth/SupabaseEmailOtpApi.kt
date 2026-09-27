package com.dumbopanda.ledger.core.network.auth

import com.dumbopanda.ledger.core.network.dto.EmailOtpErrorDto
import com.dumbopanda.ledger.core.network.dto.EmailOtpRequestDto
import com.dumbopanda.ledger.core.network.dto.EmailOtpSendOkDto
import com.dumbopanda.ledger.core.network.dto.EmailOtpVerifyOkDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class SupabaseEmailOtpApi @Inject constructor(
    private val supabase: SupabaseClient,
) : EmailOtpApi {
    override suspend fun sendCode(email: String): EmailOtpSendOutcome {
        val (status, text) = call(EmailOtpRequestDto(action = "send", email = email)) ?: return EmailOtpSendOutcome.ServerError
        if (status in 200..299) {
            return runCatching { json.decodeFromString<EmailOtpSendOkDto>(text) }
                .fold({ EmailOtpSendOutcome.Sent(it.expiresInS, it.resendInS) }, { EmailOtpSendOutcome.ServerError })
        }
        val error = decodeError(text) ?: return EmailOtpSendOutcome.ServerError
        return EmailOtpSendOutcome.Rejected(error.error, error.retryAfterS)
    }

    override suspend fun verifyCode(email: String, code: String): EmailOtpVerifyOutcome {
        val (status, text) = call(EmailOtpRequestDto(action = "verify", email = email, code = code)) ?: return EmailOtpVerifyOutcome.ServerError
        if (status in 200..299) {
            return runCatching { json.decodeFromString<EmailOtpVerifyOkDto>(text) }
                .fold(
                    { EmailOtpVerifyOutcome.Success(it.accessToken, it.refreshToken, it.expiresIn, it.user.id) },
                    { EmailOtpVerifyOutcome.ServerError },
                )
        }
        val error = decodeError(text) ?: return EmailOtpVerifyOutcome.ServerError
        return EmailOtpVerifyOutcome.Rejected(error.error, error.attemptsLeft)
    }

    // Every status code from every call goes through here uniformly: the function always
    // answers with a JSON body describing what happened (invalid_email, cooldown, wrong_code,
    // ...), on both success and failure, so status + raw text is all the callers above need --
    // whether that text arrived via a normal response or via a thrown RestException (whichever
    // this Supabase client build does for a non-2xx) doesn't matter once it's unwrapped here.
    private suspend fun call(body: EmailOtpRequestDto): Pair<Int, String>? = try {
        val response: HttpResponse = supabase.functions.invoke("email-otp", body)
        response.status.value to response.bodyAsText()
    } catch (e: RestException) {
        runCatching { e.statusCode to e.response.bodyAsText() }.getOrNull()
    } catch (e: IOException) {
        null
    }

    private fun decodeError(text: String): EmailOtpErrorDto? = runCatching { json.decodeFromString<EmailOtpErrorDto>(text) }.getOrNull()
}
