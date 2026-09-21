package com.dumbopanda.ledger.core.network.sync

import com.dumbopanda.ledger.core.network.dto.AccountDto
import com.dumbopanda.ledger.core.network.dto.EntryDto
import com.dumbopanda.ledger.core.network.dto.PushEntryResultDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class PushEntriesParams(@SerialName("p_rows") val pRows: List<EntryPushRow>)

@Serializable
private data class PullParams(
    @SerialName("p_since") val pSince: String? = null,
    @SerialName("p_since_id") val pSinceId: String? = null,
    @SerialName("p_limit") val pLimit: Int,
)

// D3: direct from client, RLS-enforced (SupabaseModule's own note). Accounts
// have no push_accounts RPC -- unlike entries, a single account write needs
// no cross-row atomicity, so it goes straight through Postgrest upsert.
@Singleton
class SupabaseSyncApi @Inject constructor(
    private val supabase: SupabaseClient,
) : SyncApi {
    override suspend fun pushEntries(rows: List<EntryPushRow>): SyncResult<List<PushResultRow>> = catching {
        supabase.postgrest.rpc("push_entries", PushEntriesParams(rows))
            .decodeList<PushEntryResultDto>()
            .map { it.toDomain() }
    }

    override suspend fun pushAccounts(rows: List<AccountPushRow>): SyncResult<List<PushResultRow>> = catching {
        supabase.postgrest.from("accounts")
            .upsert(rows) {
                onConflict = "user_id,client_uid"
                select()
            }
            .decodeList<AccountDto>()
            // Unlike push_entries' `xmax = 0` trick, a plain upsert doesn't
            // report insert-vs-update -- nothing in the account flow needs
            // that distinction today.
            .map { PushResultRow(clientUid = it.clientUid, id = it.id, updatedAt = it.updatedAt, wasInsert = false) }
    }

    override suspend fun pullEntries(
        sinceEpochMillis: Long?,
        sinceId: String?,
        limit: Int,
    ): SyncResult<List<EntryDto>> = catching {
        supabase.postgrest.rpc("pull_entries", PullParams(sinceEpochMillis?.toIso(), sinceId, limit))
            .decodeList()
    }

    override suspend fun pullAccounts(
        sinceEpochMillis: Long?,
        sinceId: String?,
        limit: Int,
    ): SyncResult<List<AccountDto>> = catching {
        supabase.postgrest.rpc("pull_accounts", PullParams(sinceEpochMillis?.toIso(), sinceId, limit))
            .decodeList()
    }

    private fun Long.toIso(): String = Instant.ofEpochMilli(this).toString()

    private suspend fun <T> catching(block: suspend () -> T): SyncResult<T> = try {
        SyncResult.Success(block())
    } catch (e: UnauthorizedRestException) {
        SyncResult.Unauthorized
    } catch (e: RestException) {
        if (e.statusCode in 400..499) {
            SyncResult.ClientError(e.message ?: e.error)
        } else {
            SyncResult.ServerError(e.message ?: "HTTP ${e.statusCode}")
        }
    } catch (e: IOException) {
        SyncResult.ServerError(e.message ?: "IO error")
    }
}

private fun PushEntryResultDto.toDomain() = PushResultRow(clientUid = clientUid, id = id, updatedAt = updatedAt, wasInsert = wasInsert)
