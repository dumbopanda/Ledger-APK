package com.dumbopanda.ledger.core.data.repository

import android.content.Context
import com.dumbopanda.ledger.core.database.LedgerDatabaseFactory
import com.dumbopanda.ledger.core.datastore.LedgerDataStoreFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AccountDeletionOutcome {
    data object Success : AccountDeletionOutcome
    data object NotSignedIn : AccountDeletionOutcome
    data class ServerRejected(val message: String) : AccountDeletionOutcome
}

/**
 * §7.17 steps 5-6: call `delete_my_account()`, then wipe everything local. Re-authentication
 * (step 4) is deliberately NOT this class's job and never touches [SessionStore] or the
 * Supabase client's own session state -- it's a client-side UX gate compared against
 * [AuthRepository.currentEmail] before this repository is ever called (see
 * `feature:settings`'s `DeleteAccountViewModel`), since `delete_my_account()` itself
 * (0007_account_deletion.sql) trusts the ALREADY-ambient session's JWT with no re-auth check of
 * its own. Re-running the Google sign-in exchange here to "prove" identity would risk silently
 * swapping the ambient session to whatever account Credential Manager returned, which is a
 * worse failure mode than the re-auth step it was meant to protect against.
 *
 * Deletion order matters: the server row is gone FIRST, then local state is wiped. If the local
 * wipe fails partway, the worst case is stale local files for an account that provably no
 * longer exists server-side -- [SessionStore] is cleared last and unconditionally, so the next
 * boot's "no stored session" (§7.1 row 1) sends the device to Onboarding regardless of how far
 * the wipe got.
 */
@Singleton
class AccountDeletionRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val dataStoreFactory: LedgerDataStoreFactory,
    @ApplicationContext private val context: Context,
) {
    suspend fun deleteAccountAndWipeLocalData(): AccountDeletionOutcome {
        val userId = sessionStore.read()?.userId ?: return AccountDeletionOutcome.NotSignedIn

        authRepository.deleteAccount()
            .onFailure { e -> return AccountDeletionOutcome.ServerRejected(e.message ?: "The server refused the request") }

        // Server row is gone -- from here on, every step is best-effort local cleanup, not a
        // reason to tell the user deletion failed.
        sessionDatabaseHolder.close()
        context.deleteDatabase(LedgerDatabaseFactory.fileNameFor(userId))
        dataStoreFactory.deleteUserData(userId)
        sessionStore.clear()

        return AccountDeletionOutcome.Success
    }
}
