package com.dumbopanda.ledger.core.network.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Doubles as the pending_ops payload schema for accounts. No id/user_id --
// accounts has no push_accounts RPC (unlike entries, a single account write
// needs no cross-row atomicity), so this is upserted directly against
// public.accounts and both columns are server-assigned (id defaults, user_id
// defaults to auth.uid() per 0002_accounts.sql).
@Serializable
data class AccountPushRow(
    @SerialName("client_uid") val clientUid: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
