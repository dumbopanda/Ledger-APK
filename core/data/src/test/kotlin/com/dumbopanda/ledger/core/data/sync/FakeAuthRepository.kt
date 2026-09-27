package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.data.repository.EmailCodeSendOutcome
import com.dumbopanda.ledger.core.data.repository.EmailCodeVerifyOutcome

class FakeAuthRepository(var refreshSucceeds: Boolean = true) : AuthRepository {
    var refreshCallCount = 0
        private set

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String> =
        error("not used in these tests")

    override suspend fun sendEmailCode(email: String): EmailCodeSendOutcome = error("not used in these tests")

    override suspend fun verifyEmailCode(email: String, code: String): EmailCodeVerifyOutcome = error("not used in these tests")

    override suspend fun signOut() = Unit

    override suspend fun refreshSession(): Boolean {
        refreshCallCount++
        return refreshSucceeds
    }

    override fun currentEmail(): String? = null

    override fun currentAuthProvider(): String? = null

    override fun nameHintFromProvider(): String? = null

    override fun rememberSignUpName(name: String?) = Unit

    override suspend fun deleteAccount(): Result<Unit> = error("not used in these tests")
}
