package com.dumbopanda.ledger.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.mapper.toPushRow
import com.dumbopanda.ledger.core.data.repository.SessionDatabaseHolder
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import com.dumbopanda.ledger.core.data.sync.EnqueueOutcome
import com.dumbopanda.ledger.core.data.sync.Op
import com.dumbopanda.ledger.core.data.sync.OpLog
import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.datastore.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val ENTRY = "entry"
private const val ACCOUNT = "account"

sealed interface ExportOutcome {
    data class Success(val accountCount: Int, val entryCount: Int) : ExportOutcome
    data object NotSignedIn : ExportOutcome
    data object WriteError : ExportOutcome
}

sealed interface ImportOutcome {
    data class Success(val accountsAdded: Int, val entriesAdded: Int) : ImportOutcome
    // §7.14: "Rejects a file whose userId differs, with an explanation rather than a silent
    // merge of someone else's ledger."
    data class UserMismatch(val fileUserId: String) : ImportOutcome
    data object NotSignedIn : ImportOutcome
    data object InvalidFile : ImportOutcome
    data object ReadError : ImportOutcome
}

/**
 * §7.14. Export/import via SAF -- the UI hands this a [Uri] it already picked with
 * `ACTION_CREATE_DOCUMENT`/`ACTION_OPEN_DOCUMENT`; this repository never touches storage
 * permissions or a file picker itself, only [Context.getContentResolver].
 *
 * `pendingOps` appears in the plan's own example JSON but is deliberately not part of
 * [LedgerExport] here: every imported row becomes a fresh `Insert` op regardless (§7.14: "go in
 * as pending inserts, so they reach the server, and the server's `on conflict` makes a double
 * import a no-op") -- replaying the OLD op queue verbatim would be actively wrong, since a
 * queued delete for a row that's also present in `entries` at export time is a contradiction
 * that can't arise from a fresh import.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDatabaseHolder: SessionDatabaseHolder,
    private val sessionStore: SessionStore,
    private val syncScheduler: SyncScheduler,
    private val userPrefsRepository: UserPrefsRepository,
    private val clock: LedgerClock,
) {
    suspend fun exportTo(uri: Uri): ExportOutcome {
        val userId = sessionStore.read()?.userId ?: return ExportOutcome.NotSignedIn
        val export = buildExportData(sessionDatabaseHolder.database(), userId, clock)
        val json = Json.encodeToString(export)
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: error("openOutputStream returned null")
        }
        if (written.isFailure) return ExportOutcome.WriteError
        userPrefsRepository.setLastBackupAt(clock.now())
        return ExportOutcome.Success(export.accounts.size, export.entries.size)
    }

    suspend fun importFrom(uri: Uri): ImportOutcome {
        val userId = sessionStore.read()?.userId ?: return ImportOutcome.NotSignedIn
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull() ?: return ImportOutcome.ReadError
        val export = try {
            Json.decodeFromString<LedgerExport>(text)
        } catch (e: SerializationException) {
            return ImportOutcome.InvalidFile
        }
        val outcome = applyImport(sessionDatabaseHolder.database(), clock, userId, export)
        if (outcome is ImportOutcome.Success && (outcome.accountsAdded > 0 || outcome.entriesAdded > 0)) {
            syncScheduler.onOpEnqueued(userPrefsRepository.currentSyncMode())
        }
        return outcome
    }

    // §7.14's nightly nudge condition.
    fun hasAnyEntries(): Flow<Boolean> = sessionDatabaseHolder.database().entryDao().hasAnyLiveEntries()
}

internal suspend fun buildExportData(db: LedgerDatabase, userId: String, clock: LedgerClock): LedgerExport {
    val accounts = db.accountDao().allLive()
    val entries = db.entryDao().allLive()
    val pending = db.pendingOpDao().page(Int.MAX_VALUE).size
    return LedgerExport(
        exportedAt = clock.now().toString(),
        userId = userId,
        counts = ExportCounts(entries = entries.size, accounts = accounts.size, pending = pending),
        accounts = accounts.map { it.toExported() },
        entries = entries.map { it.toExported() },
    )
}

// §7.14: "Additive only, keyed on client_uid. It never deletes anything already present."
// A clientUid already on this device is left untouched -- import never overwrites a local
// edit, it only fills in what's missing. Accounts import before entries so a freshly-imported
// entry's accountId foreign key is always already satisfied.
internal suspend fun applyImport(db: LedgerDatabase, clock: LedgerClock, currentUserId: String, export: LedgerExport): ImportOutcome {
    if (export.userId != currentUserId) return ImportOutcome.UserMismatch(export.userId)
    var accountsAdded = 0
    var entriesAdded = 0
    db.withTransaction {
        val opLog = OpLog(db.pendingOpDao(), clock)
        for (a in export.accounts) {
            if (db.accountDao().findByClientUid(a.clientUid) != null) continue
            val entity = a.toEntity()
            db.accountDao().upsert(entity)
            enqueueImportedInsert(opLog, ACCOUNT, entity.clientUid, Json.encodeToString(entity.toPushRow()))
            accountsAdded++
        }
        for (e in export.entries) {
            if (db.entryDao().findByClientUid(e.clientUid) != null) continue
            val entity = e.toEntity()
            db.entryDao().upsert(entity)
            enqueueImportedInsert(opLog, ENTRY, entity.clientUid, Json.encodeToString(entity.toPushRow()))
            entriesAdded++
        }
    }
    return ImportOutcome.Success(accountsAdded, entriesAdded)
}

// payloadJson MUST be the real wire shape (EntryPushRow/AccountPushRow via toPushRow()), not
// this file's own ExportedEntry/ExportedAccount JSON -- Pusher decodes it straight into that
// wire DTO and sends it as-is (§4.6/§5.1). This helper exists only to keep the enqueue call
// symmetric with every other write path's OpLog.enqueue(Op.Insert(...)) shape.
private suspend fun enqueueImportedInsert(opLog: OpLog, entityType: String, clientUid: String, payloadJson: String) {
    val outcome = opLog.enqueue(Op.Insert(entityType, clientUid, payloadJson))
    check(outcome == EnqueueOutcome.QUEUED) { "Insert can only be Append/QUEUED -- see OpCoalescer" }
}
