package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.data.repository.AuthRepository

class FakeAuthRepository(var refreshSucceeds: Boolean = true) : AuthRepository {
    var refreshCallCount = 0
        private set

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): Result<String> =
        error("not used in these tests")

    override suspend fun signOut() = Unit

    override suspend fun refreshSession(): Boolean {
        refreshCallCount++
        return refreshSucceeds
    }
}
