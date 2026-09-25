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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val ACCOUNT = "account"

// §2: "Seeded Domestic / International / Cash on first run." Order matters -- this is also
// the initial sortOrder.
private val SEED_NAMES = listOf("Domestic", "International", "Cash")

/**
 * §7.5.4's account chips need a reactive source of live (non-archived) accounts; §7.12's
 * Accounts screen (C6) needs everything else here -- rename, archive/unarchive, reorder, and
 * [allAccounts] (archived included, so the screen can still show and unarchive them). Every
 * write shares [EntryRepository]'s pattern: the entity write and its op enqueue share one Room
 * transaction, per [OpLog]'s own contract.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val clock: LedgerClock,
) {
    // §7.5.4: "generated from live accounts, never disabled" -- active means not archived.
    fun activeAccounts(): Flow<List<Account>> = activeAccounts { sessionDatabaseHolder.database() }

    // §7.12: the Accounts screen itself shows archived accounts too (to unarchive them) --
    // "archived accounts leave the chip rows but existing entries keep their reference and
    // still display."
    fun allAccounts(): Flow<List<Account>> = allAccounts { sessionDatabaseHolder.database() }

    suspend fun addAccount(account: Account) = writeAccount(sessionDatabaseHolder.database(), clock, account)

    suspend fun renameAccount(id: String, name: String) =
        renameAccount(sessionDatabaseHolder.database(), clock, id, name.trim())

    suspend fun setArchived(id: String, archived: Boolean) =
        setArchived(sessionDatabaseHolder.database(), clock, id, archived)

    suspend fun reorder(orderedIds: List<String>) = reorder(sessionDatabaseHolder.database(), clock, orderedIds)

    // §2 / §7.2: idempotent by construction (see the internal function's own doc) -- safe to
    // call from more than one screen's ViewModel without coordinating who seeds first.
    suspend fun ensureSeeded() = ensureSeeded(sessionDatabaseHolder.database(), clock)
}

internal fun activeAccounts(databaseProvider: () -> LedgerDatabase): Flow<List<Account>> =
    databaseProvider().accountDao().observeActive().map { entities -> entities.map { it.toDomain() } }

internal fun allAccounts(databaseProvider: () -> LedgerDatabase): Flow<List<Account>> =
    databaseProvider().accountDao().observeAll().map { entities -> entities.map { it.toDomain() } }

internal suspend fun writeAccount(db: LedgerDatabase, clock: LedgerClock, account: Account) {
    db.withTransaction {
        val entity = account.toEntity(createdAt = clock.now())
        db.accountDao().upsert(entity)
        OpLog(db.pendingOpDao(), clock).enqueue(
            Op.Insert(ACCOUNT, entity.clientUid, Json.encodeToString(entity.toPushRow())),
        )
    }
}

internal suspend fun renameAccount(db: LedgerDatabase, clock: LedgerClock, id: String, name: String) {
    db.withTransaction {
        db.accountDao().rename(id, name, clock.now())
        enqueueAccountUpdate(db, clock, id)
    }
}

internal suspend fun setArchived(db: LedgerDatabase, clock: LedgerClock, id: String, archived: Boolean) {
    db.withTransaction {
        val now = clock.now()
        db.accountDao().setArchived(id, if (archived) now else null, now)
        enqueueAccountUpdate(db, clock, id)
    }
}

internal suspend fun reorder(db: LedgerDatabase, clock: LedgerClock, orderedIds: List<String>) {
    db.withTransaction {
        val now = clock.now()
        orderedIds.forEachIndexed { index, id ->
            db.accountDao().updateSortOrder(id, index, now)
            enqueueAccountUpdate(db, clock, id)
        }
    }
}

private suspend fun enqueueAccountUpdate(db: LedgerDatabase, clock: LedgerClock, id: String) {
    val entity = db.accountDao().findById(id) ?: return
    OpLog(db.pendingOpDao(), clock).enqueue(
        Op.Update(ACCOUNT, entity.clientUid, Json.encodeToString(entity.toPushRow())),
    )
}

// Checked and acted on inside ONE transaction, so two near-simultaneous callers (the Add tab
// and the Accounts tab both loading on first launch, say) can't both observe zero accounts and
// both seed -- whichever transaction commits first is the one that sticks, the second sees a
// non-zero count once it runs and does nothing.
internal suspend fun ensureSeeded(db: LedgerDatabase, clock: LedgerClock) {
    db.withTransaction {
        if (db.accountDao().countLive() > 0) return@withTransaction
        val now = clock.now()
        SEED_NAMES.forEachIndexed { index, name ->
            val account = Account(
                id = UUID.randomUUID().toString(),
                clientUid = UUID.randomUUID().toString(),
                name = name,
                sortOrder = index,
                archivedAt = null,
            )
            val entity = account.toEntity(createdAt = now)
            db.accountDao().upsert(entity)
            OpLog(db.pendingOpDao(), clock).enqueue(
                Op.Insert(ACCOUNT, entity.clientUid, Json.encodeToString(entity.toPushRow())),
            )
        }
    }
}
