package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Matches pull_entries()/push_entries()'s row shape (0006_rpc.sql) exactly —
// same column names as public.entries (0003_entries.sql). No userId: RLS
// scopes every row server-side, so it's never present in the response.
@Serializable
data class EntryDto(
    val id: String,
    @SerialName("client_uid") val clientUid: String,
    @SerialName("entry_date") val entryDate: String,
    @SerialName("txn_id") val txnId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("expense_name") val expenseName: String,
    @SerialName("more_info") val moreInfo: String? = null,
    @SerialName("txn_type") val txnType: String,
    @SerialName("txn_freq") val txnFreq: String,
    @SerialName("amount_minor") val amountMinor: Long,
    val direction: String,
    @SerialName("transfer_group") val transferGroup: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

// push_entries()'s RETURNS TABLE shape — deliberately not the full row (§6.4
// commentary: do_update rather than do_nothing exists precisely so the client
// can tell "already had it" apart from "request failed").
@Serializable
data class PushEntryResultDto(
    @SerialName("client_uid") val clientUid: String,
    val id: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("was_insert") val wasInsert: Boolean,
)
