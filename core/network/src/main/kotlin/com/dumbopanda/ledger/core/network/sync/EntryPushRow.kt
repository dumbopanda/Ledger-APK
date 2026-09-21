package com.dumbopanda.ledger.core.network.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Doubles as the pending_ops payload schema (§5.1) AND the literal wire shape
// of one element of push_entries' `p_rows` array (0006_rpc.sql's
// jsonb_to_recordset columns) -- one JSON shape, no re-mapping step. Even a
// Delete op carries every field: push_entries treats every row as an upsert,
// so a delete with stale other-column values would clobber them on conflict.
@Serializable
data class EntryPushRow(
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
    @SerialName("deleted_at") val deletedAt: String? = null,
)
