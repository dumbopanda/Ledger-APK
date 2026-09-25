package com.dumbopanda.ledger.core.data.backup

import kotlinx.serialization.Serializable

/**
 * §7.14's export file shape, literally. [version] is this schema's version (currently 2,
 * matching the plan's own JSON example -- bumped only if a future field addition needs to be
 * distinguished from this one, not on every change). `pendingOps` from the plan's own example
 * is deliberately NOT part of this shape -- see [BackupRepository]'s doc for why.
 */
@Serializable
data class LedgerExport(
    val app: String = "ledger",
    val version: Int = 2,
    val exportedAt: String,
    val userId: String,
    val counts: ExportCounts,
    val accounts: List<ExportedAccount>,
    val entries: List<ExportedEntry>,
)

@Serializable
data class ExportCounts(val entries: Int, val accounts: Int, val pending: Int)

@Serializable
data class ExportedAccount(
    val id: String,
    val clientUid: String,
    val name: String,
    val sortOrder: Int,
    val archivedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ExportedEntry(
    val id: String,
    val clientUid: String,
    val entryDate: String,
    val txnId: String,
    val accountId: String,
    val expenseName: String,
    val moreInfo: String?,
    val txnType: String,
    val txnFreq: String,
    val amountMinor: Long,
    val direction: String,
    val transferGroup: String?,
    val createdAt: String,
    val updatedAt: String,
)
