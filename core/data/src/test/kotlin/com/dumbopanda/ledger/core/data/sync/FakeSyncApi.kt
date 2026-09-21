package com.dumbopanda.ledger.core.data.sync

import com.dumbopanda.ledger.core.network.dto.AccountDto
import com.dumbopanda.ledger.core.network.dto.EntryDto
import com.dumbopanda.ledger.core.network.sync.AccountPushRow
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import com.dumbopanda.ledger.core.network.sync.PushResultRow
import com.dumbopanda.ledger.core.network.sync.SyncApi
import com.dumbopanda.ledger.core.network.sync.SyncResult
import java.time.Instant
import java.util.UUID

// Simulates 0006_rpc.sql's server-side semantics closely enough to exercise
// Pusher/Puller and the B7 property test without a network: upsert-by-
// client_uid, atomic-per-call (one bad row fails the WHOLE call, matching
// push_entries being a single SQL statement), deleted_at only ever advances
// (coalesce), and keyset pull ordered by (updated_at, id).
class FakeSyncApi : SyncApi {
    private data class Row(
        val id: String,
        val clientUid: String,
        val fields: Map<String, String?>,
        val deletedAt: String?,
        val updatedAt: Instant,
    )

    private val entries = mutableMapOf<String, Row>() // keyed by clientUid
    private val accounts = mutableMapOf<String, Row>()
    private var clockTick = 0L

    // Single-shot overrides consumed by the next matching call, for testing
    // 401/4xx/5xx handling without needing a real failure to occur.
    var nextPushResult: SyncResult<List<PushResultRow>>? = null
    var nextPullResult: SyncResult<Nothing>? = null

    // Returns a message (making the row invalid) or null (valid). Runs over
    // every row in a push call -- one invalid row fails the whole call.
    var entryValidator: (EntryPushRow) -> String? = { null }

    var pushEntriesCallCount = 0
        private set

    override suspend fun pushEntries(rows: List<EntryPushRow>): SyncResult<List<PushResultRow>> {
        pushEntriesCallCount++
        nextPushResult?.let { nextPushResult = null; return it }
        for (row in rows) entryValidator(row)?.let { return SyncResult.ClientError(it) }

        return SyncResult.Success(
            rows.map { row ->
                upsert(
                    store = entries,
                    clientUid = row.clientUid,
                    deletedAt = row.deletedAt,
                    fields = mapOf(
                        "entry_date" to row.entryDate,
                        "txn_id" to row.txnId,
                        "account_id" to row.accountId,
                        "expense_name" to row.expenseName,
                        "more_info" to row.moreInfo,
                        "txn_type" to row.txnType,
                        "txn_freq" to row.txnFreq,
                        "amount_minor" to row.amountMinor.toString(),
                        "direction" to row.direction,
                        "transfer_group" to row.transferGroup,
                    ),
                )
            },
        )
    }

    override suspend fun pushAccounts(rows: List<AccountPushRow>): SyncResult<List<PushResultRow>> {
        nextPushResult?.let { nextPushResult = null; return it }

        return SyncResult.Success(
            rows.map { row ->
                upsert(
                    store = accounts,
                    clientUid = row.clientUid,
                    deletedAt = row.deletedAt,
                    fields = mapOf(
                        "name" to row.name,
                        "sort_order" to row.sortOrder.toString(),
                        "archived_at" to row.archivedAt,
                    ),
                )
            },
        )
    }

    var pullEntriesCallCount = 0
        private set

    override suspend fun pullEntries(sinceEpochMillis: Long?, sinceId: String?, limit: Int): SyncResult<List<EntryDto>> {
        pullEntriesCallCount++
        nextPullResult?.let { nextPullResult = null; return it }
        return SyncResult.Success(
            page(entries, sinceEpochMillis, sinceId, limit).map { row ->
                EntryDto(
                    id = row.id,
                    clientUid = row.clientUid,
                    entryDate = row.fields["entry_date"]!!,
                    txnId = row.fields["txn_id"]!!,
                    accountId = row.fields["account_id"]!!,
                    expenseName = row.fields["expense_name"]!!,
                    moreInfo = row.fields["more_info"],
                    txnType = row.fields["txn_type"]!!,
                    txnFreq = row.fields["txn_freq"]!!,
                    amountMinor = row.fields["amount_minor"]!!.toLong(),
                    direction = row.fields["direction"]!!,
                    transferGroup = row.fields["transfer_group"],
                    createdAt = row.updatedAt.toString(),
                    updatedAt = row.updatedAt.toString(),
                    deletedAt = row.deletedAt,
                )
            },
        )
    }

    override suspend fun pullAccounts(sinceEpochMillis: Long?, sinceId: String?, limit: Int): SyncResult<List<AccountDto>> {
        nextPullResult?.let { nextPullResult = null; return it }
        return SyncResult.Success(
            page(accounts, sinceEpochMillis, sinceId, limit).map { row ->
                AccountDto(
                    id = row.id,
                    clientUid = row.clientUid,
                    name = row.fields["name"]!!,
                    sortOrder = row.fields["sort_order"]!!.toInt(),
                    archivedAt = row.fields["archived_at"],
                    createdAt = row.updatedAt.toString(),
                    updatedAt = row.updatedAt.toString(),
                    deletedAt = row.deletedAt,
                )
            },
        )
    }

    private fun upsert(
        store: MutableMap<String, Row>,
        clientUid: String,
        deletedAt: String?,
        fields: Map<String, String?>,
    ): PushResultRow {
        val existing = store[clientUid]
        val wasInsert = existing == null
        val updatedAt = Instant.ofEpochMilli(clockTick++)
        val row = Row(
            id = existing?.id ?: UUID.randomUUID().toString(),
            clientUid = clientUid,
            fields = fields,
            // A delete is never undone by a push (0006_rpc.sql's own rule).
            deletedAt = existing?.deletedAt ?: deletedAt,
            updatedAt = updatedAt,
        )
        store[clientUid] = row
        return PushResultRow(clientUid = clientUid, id = row.id, updatedAt = updatedAt.toString(), wasInsert = wasInsert)
    }

    private fun page(store: Map<String, Row>, sinceEpochMillis: Long?, sinceId: String?, limit: Int): List<Row> {
        val since = sinceEpochMillis?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH.minusMillis(1)
        val sinceIdOrEmpty = sinceId ?: ""
        return store.values
            .filter { (it.updatedAt to it.id) > (since to sinceIdOrEmpty) }
            .sortedWith(compareBy({ it.updatedAt }, { it.id }))
            .take(limit)
    }
}

private operator fun <A : Comparable<A>, B : Comparable<B>> Pair<A, B>.compareTo(other: Pair<A, B>): Int {
    val firstCompare = first.compareTo(other.first)
    return if (firstCompare != 0) firstCompare else second.compareTo(other.second)
}
