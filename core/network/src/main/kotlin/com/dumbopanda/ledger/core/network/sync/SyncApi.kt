package com.dumbopanda.ledger.core.network.sync

import com.dumbopanda.ledger.core.network.dto.AccountDto
import com.dumbopanda.ledger.core.network.dto.EntryDto

// §5.3/§5.4's push and pull, as a testable boundary. Pusher/Puller (core:data)
// depend on this interface, not on SupabaseClient directly, so their retry
// and coalescing logic can be property-tested against a fake backend without
// a network.
interface SyncApi {
    suspend fun pushEntries(rows: List<EntryPushRow>): SyncResult<List<PushResultRow>>
    suspend fun pushAccounts(rows: List<AccountPushRow>): SyncResult<List<PushResultRow>>

    suspend fun pullEntries(sinceEpochMillis: Long?, sinceId: String?, limit: Int): SyncResult<List<EntryDto>>
    suspend fun pullAccounts(sinceEpochMillis: Long?, sinceId: String?, limit: Int): SyncResult<List<AccountDto>>
}
