package com.dumbopanda.ledger.core.data.repository

import androidx.room.withTransaction
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.mapper.toDomain
import com.dumbopanda.ledger.core.data.mapper.toEntity
import com.dumbopanda.ledger.core.data.mapper.toPushRow
import com.dumbopanda.ledger.core.data.sync.Op
import com.dumbopanda.ledger.core.data.sync.OpLog
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val ACCOUNT = "account"

/**
 * §7.5.4's account chips need SOME reactive source of live (non-archived) accounts to render
 * at all -- this is that source, plus the one write path that exists so far (adding an
 * account, needed for §7.12's screen later and for the earliest possible manual smoke test of
 * the account-chip flow before seeding, C6, exists). Follows [EntryRepository]'s pattern: an
 * entity write and its op enqueue share one Room transaction, per [OpLog]'s own contract.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val clock: LedgerClock,
) {
    // §7.5.4: "generated from live accounts, never disabled" -- active means not archived.
    fun activeAccounts(): Flow<List<Account>> = activeAccounts { sessionDatabaseHolder.database() }

    suspend fun addAccount(account: Account) = writeAccount(sessionDatabaseHolder.database(), clock, account)
}

internal fun activeAccounts(databaseProvider: () -> LedgerDatabase): Flow<List<Account>> =
    databaseProvider().accountDao().observeActive().map { entities -> entities.map { it.toDomain() } }

internal suspend fun writeAccount(db: LedgerDatabase, clock: LedgerClock, account: Account) {
    db.withTransaction {
        val entity = account.toEntity(createdAt = clock.now())
        db.accountDao().upsert(entity)
        OpLog(db.pendingOpDao(), clock).enqueue(
            Op.Insert(ACCOUNT, entity.clientUid, Json.encodeToString(entity.toPushRow())),
        )
    }
}
