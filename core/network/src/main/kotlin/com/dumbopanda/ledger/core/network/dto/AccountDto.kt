package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Matches pull_accounts()'s row shape (0006_rpc.sql) — same columns as
// public.accounts (0002_accounts.sql), no userId for the same RLS reason.
@Serializable
data class AccountDto(
    val id: String,
    @SerialName("client_uid") val clientUid: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
