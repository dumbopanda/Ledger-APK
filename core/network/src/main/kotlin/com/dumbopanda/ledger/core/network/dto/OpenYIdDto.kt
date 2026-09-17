package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// open_y_ids()'s shape — reconciliation only (§7.9); the app computes this
// locally from EntryDao.openYIds(), this DTO exists for the drift check.
@Serializable
data class OpenYIdDto(
    @SerialName("txn_id") val txnId: String,
    @SerialName("leg_count") val legCount: Long,
)
